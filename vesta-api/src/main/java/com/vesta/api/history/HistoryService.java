package com.vesta.api.history;

import com.vesta.api.common.error.BadRequestException;
import com.vesta.api.common.error.NotFoundException;
import com.vesta.api.history.HistoryDtos.DailyStat;
import com.vesta.api.history.HistoryDtos.DayState;
import com.vesta.api.history.HistoryDtos.HistoryDay;
import com.vesta.api.history.HistoryDtos.HistoryResponse;
import com.vesta.api.history.HistoryDtos.HistorySession;
import com.vesta.api.history.HistoryDtos.StatsSummary;
import com.vesta.api.history.HistoryDtos.StreakResponse;
import com.vesta.api.session.FocusSession;
import com.vesta.api.session.FocusSessionRepository;
import com.vesta.api.session.SessionStatus;
import com.vesta.api.user.UserAccount;
import com.vesta.api.user.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {

    private final FocusSessionRepository sessions;
    private final UserRepository users;
    private final Clock clock;

    public HistoryService(FocusSessionRepository sessions, UserRepository users, Clock clock) {
        this.sessions = sessions;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public HistoryResponse history(UUID userId, LocalDate from, LocalDate to) {
        DateRange range = range(userId, from, to, 14);
        Map<LocalDate, List<FocusSession>> byDate = sessions.findBetween(userId, range.from, range.to)
                .stream().collect(Collectors.groupingBy(FocusSession::getLocalDate));
        List<HistoryDay> days = range.from.datesUntil(range.to.plusDays(1))
                .map(date -> historyDay(date, byDate.getOrDefault(date, List.of())))
                .toList();
        return new HistoryResponse(range.from, range.to, days);
    }

    @Transactional(readOnly = true)
    public StreakResponse streak(UUID userId) {
        UserAccount user = findUser(userId);
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(user.getTimezone())));
        List<LocalDate> dates = sessions.findCompletedDates(userId).stream().distinct().toList();
        if (dates.isEmpty()) return new StreakResponse(0, 0, null);

        int longest = 1;
        int run = 1;
        for (int index = 1; index < dates.size(); index++) {
            if (dates.get(index - 1).plusDays(1).equals(dates.get(index))) {
                run++;
                longest = Math.max(longest, run);
            } else {
                run = 1;
            }
        }

        LocalDate last = dates.get(dates.size() - 1);
        int current = 0;
        if (!last.isBefore(today.minusDays(1))) {
            current = 1;
            for (int index = dates.size() - 1; index > 0; index--) {
                if (dates.get(index - 1).plusDays(1).equals(dates.get(index))) current++;
                else break;
            }
        }
        return new StreakResponse(current, longest, last);
    }

    @Transactional(readOnly = true)
    public StatsSummary stats(UUID userId, LocalDate from, LocalDate to) {
        DateRange range = range(userId, from, to, 7);
        List<FocusSession> values = sessions.findBetween(userId, range.from, range.to);
        long completed = values.stream().filter(value -> value.getStatus() == SessionStatus.COMPLETED).count();
        long abandoned = values.stream().filter(value -> value.getStatus() == SessionStatus.ABANDONED).count();
        long focused = values.stream().filter(value -> value.getStatus() == SessionStatus.COMPLETED)
                .mapToLong(value -> value.getActualFocusSeconds() == null ? 0 : value.getActualFocusSeconds())
                .sum();
        Map<String, Long> byTask = values.stream()
                .filter(value -> value.getStatus() == SessionStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        value -> value.getTaskTitleSnapshot() == null ? "Free focus" : value.getTaskTitleSnapshot(),
                        LinkedHashMap::new,
                        Collectors.summingLong(value -> value.getActualFocusSeconds() == null
                                ? 0 : value.getActualFocusSeconds())));
        Map<LocalDate, List<FocusSession>> byDate = values.stream()
                .collect(Collectors.groupingBy(FocusSession::getLocalDate));
        List<DailyStat> daily = range.from.datesUntil(range.to.plusDays(1)).map(date -> {
            List<FocusSession> day = byDate.getOrDefault(date, List.of());
            return new DailyStat(date,
                    day.stream().filter(value -> value.getStatus() == SessionStatus.COMPLETED).count(),
                    day.stream().filter(value -> value.getStatus() == SessionStatus.COMPLETED)
                            .mapToLong(value -> value.getActualFocusSeconds() == null
                                    ? 0 : value.getActualFocusSeconds()).sum());
        }).toList();
        return new StatsSummary(range.from, range.to, completed, abandoned, focused, byTask, daily);
    }

    private HistoryDay historyDay(LocalDate date, List<FocusSession> values) {
        List<HistorySession> items = values.stream().map(value -> new HistorySession(value.getId(),
                value.getTask() == null ? null : value.getTask().getId(), value.getTaskTitleSnapshot(),
                value.getStatus(), value.getPlannedDurationSeconds(), value.getActualFocusSeconds(),
                value.getStartedAt(), value.getEndedAt())).toList();
        int completed = (int) values.stream().filter(value -> value.getStatus() == SessionStatus.COMPLETED).count();
        int abandoned = (int) values.stream().filter(value -> value.getStatus() == SessionStatus.ABANDONED).count();
        long seconds = values.stream().filter(value -> value.getStatus() == SessionStatus.COMPLETED)
                .mapToLong(value -> value.getActualFocusSeconds() == null ? 0 : value.getActualFocusSeconds()).sum();
        DayState state = completed > 0 ? DayState.LIT : abandoned > 0 ? DayState.ASH : DayState.EMPTY;
        return new HistoryDay(date, state, completed, abandoned, seconds, items);
    }

    private DateRange range(UUID userId, LocalDate requestedFrom, LocalDate requestedTo, int defaultDays) {
        UserAccount user = findUser(userId);
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(user.getTimezone())));
        LocalDate to = requestedTo == null ? today : requestedTo;
        LocalDate from = requestedFrom == null ? to.minusDays(defaultDays - 1L) : requestedFrom;
        long days = ChronoUnit.DAYS.between(from, to);
        if (days < 0 || days > 365) {
            throw new BadRequestException("invalid_date_range", "Date range must contain 1 to 366 days.");
        }
        return new DateRange(from, to);
    }

    private UserAccount findUser(UUID userId) {
        return users.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("user_not_found", "User not found."));
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}

