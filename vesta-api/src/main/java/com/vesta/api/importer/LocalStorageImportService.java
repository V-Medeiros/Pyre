package com.vesta.api.importer;

import com.vesta.api.common.error.BadRequestException;
import com.vesta.api.common.error.NotFoundException;
import com.vesta.api.importer.ImportDtos.ImportResponse;
import com.vesta.api.importer.ImportDtos.LocalActiveSession;
import com.vesta.api.importer.ImportDtos.LocalSession;
import com.vesta.api.importer.ImportDtos.LocalStorageImportRequest;
import com.vesta.api.importer.ImportDtos.LocalTask;
import com.vesta.api.preferences.Theme;
import com.vesta.api.preferences.UserPreferences;
import com.vesta.api.preferences.UserPreferencesRepository;
import com.vesta.api.session.FocusSession;
import com.vesta.api.session.FocusSessionRepository;
import com.vesta.api.session.SessionEvent;
import com.vesta.api.session.SessionEventRepository;
import com.vesta.api.session.SessionStatus;
import com.vesta.api.task.Task;
import com.vesta.api.task.TaskRepository;
import com.vesta.api.user.UserAccount;
import com.vesta.api.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalStorageImportService {

    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");
    private final ImportBatchRepository batches;
    private final UserRepository users;
    private final TaskRepository tasks;
    private final FocusSessionRepository sessions;
    private final SessionEventRepository events;
    private final UserPreferencesRepository preferences;
    private final Clock clock;

    public LocalStorageImportService(ImportBatchRepository batches, UserRepository users,
                                     TaskRepository tasks, FocusSessionRepository sessions,
                                     SessionEventRepository events,
                                     UserPreferencesRepository preferences, Clock clock) {
        this.batches = batches;
        this.users = users;
        this.tasks = tasks;
        this.sessions = sessions;
        this.events = events;
        this.preferences = preferences;
        this.clock = clock;
    }

    @Transactional
    public ImportResponse importData(UUID userId, String idempotencyKey,
                                     LocalStorageImportRequest request) {
        validateKey(idempotencyKey);
        ImportBatch replay = batches.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElse(null);
        if (replay != null) return ImportResponse.from(replay, true);

        UserAccount user = users.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("user_not_found", "User not found."));
        Counter counter = new Counter();
        for (LocalTask task : safe(request.tasks())) importTask(user, task, counter);
        for (LocalSession session : safe(request.sessions())) importSession(user, session, counter);
        importSettings(userId, request, counter);
        importActive(user, request.activeSession(), counter);
        if (request.streak() != null) counter.ignored++;
        if (request.stopwatch() != null) counter.ignored++;

        ImportBatch batch = batches.save(new ImportBatch(user, idempotencyKey, counter.tasks,
                counter.sessions, counter.ignored, counter.invalid, Instant.now(clock)));
        return ImportResponse.from(batch, false);
    }

    @Transactional(readOnly = true)
    public ImportResponse get(UUID userId, UUID importId) {
        ImportBatch batch = batches.findByIdAndUserId(importId, userId)
                .orElseThrow(() -> new NotFoundException("import_not_found", "Import not found."));
        return ImportResponse.from(batch, false);
    }

    private void importTask(UserAccount user, LocalTask value, Counter counter) {
        try {
            if (value == null || !validId(value.id()) || value.text() == null
                    || value.text().trim().isEmpty() || value.text().trim().length() > 120) {
                counter.invalid++;
                return;
            }
            Task existing = tasks.findById(value.id()).orElse(null);
            if (existing != null) {
                if (existing.getUser().getId().equals(user.getId())) counter.ignored++;
                else counter.invalid++;
                return;
            }
            Instant created = parse(value.createdAt());
            Instant updated = parse(value.updatedAt());
            tasks.save(Task.imported(value.id(), user, value.text().trim(),
                    Boolean.TRUE.equals(value.completed()), created, updated));
            counter.tasks++;
        } catch (RuntimeException exception) {
            counter.invalid++;
        }
    }

    private void importSession(UserAccount user, LocalSession value, Counter counter) {
        try {
            if (value == null || !validId(value.id()) || value.durationMinutes() == null
                    || value.startedAt() == null || value.endedAt() == null) {
                counter.invalid++;
                return;
            }
            FocusSession existing = sessions.findById(value.id()).orElse(null);
            if (existing != null) {
                if (existing.getUser().getId().equals(user.getId())) counter.ignored++;
                else counter.invalid++;
                return;
            }
            int minutes = value.durationMinutes().intValue();
            if (minutes < 5 || minutes > 120) throw new IllegalArgumentException();
            SessionStatus status = switch (value.status().toLowerCase(Locale.ROOT)) {
                case "completed" -> SessionStatus.COMPLETED;
                case "abandoned" -> SessionStatus.ABANDONED;
                default -> throw new IllegalArgumentException();
            };
            Instant started = parse(value.startedAt());
            Instant ended = parse(value.endedAt());
            if (ended.isBefore(started) || ended.isAfter(Instant.now(clock).plusSeconds(300))) {
                throw new IllegalArgumentException();
            }
            Task task = value.taskId() == null ? null
                    : tasks.findOwnedIncludingDeleted(value.taskId(), user.getId()).orElse(null);
            int planned = minutes * 60;
            int actual = status == SessionStatus.COMPLETED ? planned
                    : (int) Math.min(planned, Math.max(0, Duration.between(started, ended).toSeconds()));
            FocusSession imported = FocusSession.imported(value.id(), user, task,
                    task == null && value.taskId() != null ? "Deleted task" : task == null ? null : task.getTitle(),
                    status, planned, actual, started, ended, user.getTimezone());
            sessions.save(imported);
            events.save(new SessionEvent(imported, "IMPORTED", Instant.now(clock), "local-storage"));
            counter.sessions++;
        } catch (RuntimeException exception) {
            counter.invalid++;
        }
    }

    private void importSettings(UUID userId, LocalStorageImportRequest request, Counter counter) {
        if (request.settings() == null && request.theme() == null) return;
        try {
            UserPreferences value = preferences.findById(userId).orElseThrow();
            Integer duration = request.settings() == null || request.settings().defaultDuration() == null
                    ? null : request.settings().defaultDuration().intValue();
            if (duration != null && (duration < 5 || duration > 120)) throw new IllegalArgumentException();
            Boolean sound = request.settings() == null ? null : request.settings().soundEnabled();
            Theme theme = request.theme() == null ? null
                    : Theme.valueOf(request.theme().toUpperCase(Locale.ROOT));
            value.update(duration, sound, theme, Instant.now(clock));
        } catch (RuntimeException exception) {
            counter.invalid++;
        }
    }

    private void importActive(UserAccount user, LocalActiveSession value, Counter counter) {
        if (value == null) return;
        try {
            if (!validId(value.id()) || value.durationMinutes() == null || value.startedAt() == null
                    || !("running".equals(value.status()) || "paused".equals(value.status()))) {
                throw new IllegalArgumentException();
            }
            if (sessions.findById(value.id()).isPresent() || sessions.findActive(user.getId()).isPresent()) {
                counter.ignored++;
                return;
            }
            int minutes = value.durationMinutes().intValue();
            if (minutes < 5 || minutes > 120) throw new IllegalArgumentException();
            Task task = value.taskId() == null ? null
                    : tasks.findActiveOwned(value.taskId(), user.getId()).orElse(null);
            Instant now = Instant.now(clock);
            Instant deadline = value.endsAt() == null ? null
                    : Instant.ofEpochMilli(value.endsAt().longValue());
            Integer remaining = value.pausedSecondsRemaining() == null ? null
                    : value.pausedSecondsRemaining().intValue();
            SessionStatus status = "running".equals(value.status())
                    ? SessionStatus.RUNNING : SessionStatus.PAUSED;
            FocusSession imported = FocusSession.importedActive(value.id(), user, task, minutes * 60,
                    status, parse(value.startedAt()), deadline, remaining, now);
            sessions.save(imported);
            events.save(new SessionEvent(imported, "IMPORTED", now, "local-storage"));
            counter.sessions++;
        } catch (RuntimeException exception) {
            counter.invalid++;
        }
    }

    private Instant parse(String value) {
        return Instant.parse(value);
    }

    private boolean validId(String value) {
        return value != null && ID.matcher(value).matches();
    }

    private void validateKey(String value) {
        if (value == null || !value.matches("^[A-Za-z0-9_.-]{8,100}$")) {
            throw new BadRequestException("invalid_idempotency_key",
                    "Idempotency-Key must contain 8 to 100 safe characters.");
        }
    }

    private <T> List<T> safe(List<T> value) {
        return value == null ? List.of() : value;
    }

    private static final class Counter {
        int tasks;
        int sessions;
        int ignored;
        int invalid;
    }
}
