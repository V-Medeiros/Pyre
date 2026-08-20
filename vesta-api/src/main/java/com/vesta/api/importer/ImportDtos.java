package com.vesta.api.importer;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ImportDtos {

    private ImportDtos() {
    }

    public record LocalTask(
            String id,
            String text,
            Boolean completed,
            Number sessionsCount,
            String createdAt,
            String updatedAt) {
    }

    public record LocalSession(
            String id,
            String date,
            Number durationMinutes,
            String taskId,
            String status,
            String startedAt,
            String endedAt) {
    }

    public record LocalSettings(Number defaultDuration, Boolean soundEnabled) {
    }

    public record LocalActiveSession(
            String id,
            Number durationMinutes,
            String taskId,
            String status,
            String startedAt,
            Number endsAt,
            Number pausedSecondsRemaining) {
    }

    public record LocalStorageImportRequest(
            @Size(max = 10_000) List<LocalTask> tasks,
            @Size(max = 50_000) List<LocalSession> sessions,
            JsonNode streak,
            LocalSettings settings,
            LocalActiveSession activeSession,
            JsonNode stopwatch,
            String theme) {
    }

    public record ImportResponse(
            UUID importId,
            int importedTasks,
            int importedSessions,
            int ignoredItems,
            int invalidItems,
            boolean replayed,
            Instant createdAt) {
        static ImportResponse from(ImportBatch batch, boolean replayed) {
            return new ImportResponse(batch.getId(), batch.getImportedTasks(), batch.getImportedSessions(),
                    batch.getIgnoredItems(), batch.getInvalidItems(), replayed, batch.getCreatedAt());
        }
    }
}

