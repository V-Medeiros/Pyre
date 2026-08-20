package com.vesta.api.task;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class TaskDtos {

    private TaskDtos() {
    }

    public enum TaskFilter { OPEN, COMPLETED, ALL }

    public record CreateTaskRequest(
            @Pattern(regexp = "^[A-Za-z0-9_-]{1,100}$") String id,
            @NotBlank @Size(max = 120) String title) {
    }

    public record UpdateTaskRequest(
            @NotBlank @Size(max = 120) String title,
            @PositiveOrZero Long version) {
    }

    public record VersionRequest(@PositiveOrZero Long version) {
    }

    public record TaskResponse(
            String id,
            String title,
            boolean completed,
            long sessionsCount,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }

    public record TaskPage(List<TaskResponse> items, String nextCursor) {
    }
}

