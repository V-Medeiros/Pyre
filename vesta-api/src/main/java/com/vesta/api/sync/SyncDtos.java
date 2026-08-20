package com.vesta.api.sync;

import com.vesta.api.preferences.Theme;
import com.vesta.api.session.SessionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class SyncDtos {

    private SyncDtos() {
    }

    public record SyncTask(
            String id,
            String title,
            boolean completed,
            boolean deleted,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }

    public record SyncSession(
            String id,
            String taskId,
            String taskTitle,
            SessionStatus status,
            int plannedDurationSeconds,
            int accumulatedFocusSeconds,
            Integer actualFocusSeconds,
            Instant startedAt,
            Instant deadlineAt,
            Instant endedAt,
            String timezone,
            LocalDate localDate,
            Instant updatedAt,
            long version) {
    }

    public record SyncPreferences(
            int defaultDurationMinutes,
            boolean soundEnabled,
            Theme theme,
            Instant updatedAt,
            long version) {
    }

    public record SyncResponse(
            String cursor,
            List<SyncTask> tasks,
            List<SyncSession> sessions,
            SyncPreferences preferences) {
    }
}
