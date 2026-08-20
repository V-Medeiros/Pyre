package com.vesta.api.history;

import com.vesta.api.session.SessionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class HistoryDtos {

    private HistoryDtos() {
    }

    public enum DayState { LIT, ASH, EMPTY }

    public record HistorySession(
            String id,
            String taskId,
            String taskTitle,
            SessionStatus status,
            int plannedDurationSeconds,
            Integer actualFocusSeconds,
            Instant startedAt,
            Instant endedAt) {
    }

    public record HistoryDay(
            LocalDate date,
            DayState state,
            int completedSessions,
            int abandonedSessions,
            long focusedSeconds,
            List<HistorySession> sessions) {
    }

    public record HistoryResponse(LocalDate from, LocalDate to, List<HistoryDay> days) {
    }

    public record StreakResponse(int current, int longestEver, LocalDate lastCompletedDate) {
    }

    public record DailyStat(LocalDate date, long completedSessions, long focusedSeconds) {
    }

    public record StatsSummary(
            LocalDate from,
            LocalDate to,
            long completedSessions,
            long abandonedSessions,
            long focusedSeconds,
            Map<String, Long> focusedSecondsByTask,
            List<DailyStat> daily) {
    }
}

