package com.vesta.api.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class SessionDtos {

    private SessionDtos() {
    }

    public record StartSessionRequest(
            @Pattern(regexp = "^[A-Za-z0-9_-]{1,100}$") String id,
            @Min(5) @Max(120) int durationMinutes,
            @Pattern(regexp = "^[A-Za-z0-9_-]{1,100}$") String taskId,
            @Pattern(regexp = "^[A-Za-z0-9_.-]{1,100}$") String deviceId) {
    }

    public record TransitionRequest(
            @PositiveOrZero Long version,
            @Pattern(regexp = "^[A-Za-z0-9_.-]{1,100}$") String deviceId) {
    }

    public record SessionResponse(
            String id,
            String taskId,
            String taskTitle,
            SessionStatus status,
            int plannedDurationSeconds,
            Integer actualFocusSeconds,
            int secondsRemaining,
            Instant startedAt,
            Instant deadlineAt,
            Instant endedAt,
            String timezone,
            LocalDate localDate,
            long version) {
    }

    public record SessionPage(List<SessionResponse> items, String nextCursor) {
    }
}
