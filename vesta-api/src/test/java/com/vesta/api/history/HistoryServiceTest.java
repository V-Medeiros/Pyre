package com.vesta.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vesta.api.session.FocusSessionRepository;
import com.vesta.api.user.UserAccount;
import com.vesta.api.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HistoryServiceTest {

    @Test
    void derivesCurrentAndLongestStreakFromCompletedDates() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T15:00:00Z");
        UserAccount user = new UserAccount(userId, "user@example.com", "hash", null,
                "America/Sao_Paulo", "pt-BR", now);
        FocusSessionRepository sessions = mock(FocusSessionRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(users.findActiveById(userId)).thenReturn(Optional.of(user));
        when(sessions.findCompletedDates(userId)).thenReturn(List.of(
                LocalDate.parse("2026-08-14"),
                LocalDate.parse("2026-08-15"),
                LocalDate.parse("2026-08-16"),
                LocalDate.parse("2026-08-19"),
                LocalDate.parse("2026-08-20")));
        HistoryService service = new HistoryService(sessions, users, Clock.fixed(now, ZoneOffset.UTC));

        var streak = service.streak(userId);

        assertThat(streak.current()).isEqualTo(2);
        assertThat(streak.longestEver()).isEqualTo(3);
        assertThat(streak.lastCompletedDate()).isEqualTo("2026-08-20");
    }

    @Test
    void resetsCurrentStreakAfterMissingMoreThanOneDay() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-20T15:00:00Z");
        UserAccount user = new UserAccount(userId, "user@example.com", "hash", null,
                "UTC", "en-US", now);
        FocusSessionRepository sessions = mock(FocusSessionRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(users.findActiveById(userId)).thenReturn(Optional.of(user));
        when(sessions.findCompletedDates(userId)).thenReturn(List.of(
                LocalDate.parse("2026-08-16"), LocalDate.parse("2026-08-17")));
        HistoryService service = new HistoryService(sessions, users, Clock.fixed(now, ZoneOffset.UTC));

        assertThat(service.streak(userId).current()).isZero();
        assertThat(service.streak(userId).longestEver()).isEqualTo(2);
    }
}

