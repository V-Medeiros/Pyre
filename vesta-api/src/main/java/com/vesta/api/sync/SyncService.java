package com.vesta.api.sync;

import com.vesta.api.common.error.BadRequestException;
import com.vesta.api.common.error.NotFoundException;
import com.vesta.api.preferences.UserPreferences;
import com.vesta.api.preferences.UserPreferencesRepository;
import com.vesta.api.session.FocusSession;
import com.vesta.api.session.FocusSessionRepository;
import com.vesta.api.sync.SyncDtos.SyncPreferences;
import com.vesta.api.sync.SyncDtos.SyncResponse;
import com.vesta.api.sync.SyncDtos.SyncSession;
import com.vesta.api.sync.SyncDtos.SyncTask;
import com.vesta.api.task.Task;
import com.vesta.api.task.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncService {

    private final TaskRepository tasks;
    private final FocusSessionRepository sessions;
    private final UserPreferencesRepository preferences;
    private final Clock clock;

    public SyncService(TaskRepository tasks, FocusSessionRepository sessions,
                       UserPreferencesRepository preferences, Clock clock) {
        this.tasks = tasks;
        this.sessions = sessions;
        this.preferences = preferences;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SyncResponse bootstrap(UUID userId) {
        Instant until = Instant.now(clock);
        return response(until, tasks.findAllOwned(userId), sessions.findAllOwned(userId),
                findPreferences(userId));
    }

    @Transactional(readOnly = true)
    public SyncResponse changes(UUID userId, String cursor) {
        Instant since;
        try {
            since = Instant.parse(cursor);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new BadRequestException("invalid_sync_cursor", "Sync cursor must be an ISO-8601 instant.");
        }
        Instant until = Instant.now(clock);
        if (since.isAfter(until)) {
            throw new BadRequestException("invalid_sync_cursor", "Sync cursor cannot be in the future.");
        }
        UserPreferences prefs = findPreferences(userId);
        SyncPreferences changedPreferences = prefs.getUpdatedAt().isAfter(since)
                && !prefs.getUpdatedAt().isAfter(until) ? preferences(prefs) : null;
        return new SyncResponse(until.toString(),
                tasks.findChanges(userId, since, until).stream().map(this::task).toList(),
                sessions.findChanges(userId, since, until).stream().map(this::session).toList(),
                changedPreferences);
    }

    private SyncResponse response(Instant cursor, List<Task> taskValues,
                                  List<FocusSession> sessionValues, UserPreferences prefs) {
        return new SyncResponse(cursor.toString(), taskValues.stream().map(this::task).toList(),
                sessionValues.stream().map(this::session).toList(), preferences(prefs));
    }

    private SyncTask task(Task value) {
        return new SyncTask(value.getId(), value.getTitle(), value.getCompletedAt() != null,
                value.getDeletedAt() != null, value.getCreatedAt(), value.getUpdatedAt(), value.getVersion());
    }

    private SyncSession session(FocusSession value) {
        return new SyncSession(value.getId(), value.getTask() == null ? null : value.getTask().getId(),
                value.getTaskTitleSnapshot(), value.getStatus(), value.getPlannedDurationSeconds(),
                value.getAccumulatedFocusSeconds(), value.getActualFocusSeconds(), value.getStartedAt(),
                value.getCurrentDeadlineAt(),
                value.getEndedAt(), value.getSessionTimezone(), value.getLocalDate(), value.getUpdatedAt(),
                value.getVersion());
    }

    private SyncPreferences preferences(UserPreferences value) {
        return new SyncPreferences(value.getDefaultDurationMinutes(), value.isSoundEnabled(), value.getTheme(),
                value.getUpdatedAt(), value.getVersion());
    }

    private UserPreferences findPreferences(UUID userId) {
        return preferences.findById(userId)
                .orElseThrow(() -> new NotFoundException("preferences_not_found", "Preferences not found."));
    }
}
