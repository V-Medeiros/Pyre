package com.vesta.api.session;

import com.vesta.api.common.error.ConflictException;
import com.vesta.api.common.error.NotFoundException;
import com.vesta.api.session.SessionDtos.SessionPage;
import com.vesta.api.session.SessionDtos.SessionResponse;
import com.vesta.api.session.SessionDtos.StartSessionRequest;
import com.vesta.api.session.SessionDtos.TransitionRequest;
import com.vesta.api.task.Task;
import com.vesta.api.task.TaskRepository;
import com.vesta.api.user.UserAccount;
import com.vesta.api.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final FocusSessionRepository sessions;
    private final SessionEventRepository events;
    private final UserRepository users;
    private final TaskRepository tasks;
    private final Clock clock;

    public SessionService(FocusSessionRepository sessions, SessionEventRepository events,
                          UserRepository users, TaskRepository tasks, Clock clock) {
        this.sessions = sessions;
        this.events = events;
        this.users = users;
        this.tasks = tasks;
        this.clock = clock;
    }

    @Transactional
    public SessionResponse start(UUID userId, StartSessionRequest request) {
        UserAccount user = users.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("user_not_found", "User not found."));
        String id = request.id() == null ? "session_" + UUID.randomUUID() : request.id();
        FocusSession duplicate = sessions.findById(id).orElse(null);
        if (duplicate != null) {
            if (duplicate.getUser().getId().equals(userId)
                    && duplicate.getPlannedDurationSeconds() == request.durationMinutes() * 60) {
                return response(duplicate, Instant.now(clock));
            }
            throw new ConflictException("session_id_conflict", "Session identifier is already in use.");
        }

        Instant now = Instant.now(clock);
        FocusSession active = sessions.findActive(userId).orElse(null);
        if (active != null) {
            if (active.reconcile(now)) {
                sessions.save(active);
                events.save(new SessionEvent(active, "COMPLETED", now, "server"));
            } else {
                throw new ConflictException("active_session_exists", "Finish the active session first.");
            }
        }

        Task task = null;
        if (request.taskId() != null) {
            task = tasks.findActiveOwned(request.taskId(), userId)
                    .orElseThrow(() -> new NotFoundException("task_not_found", "Task not found."));
            if (task.getCompletedAt() != null) {
                throw new ConflictException("task_completed", "A completed task cannot start a session.");
            }
        }

        FocusSession session = sessions.save(FocusSession.start(id, user, task,
                request.durationMinutes() * 60, now));
        events.save(new SessionEvent(session, "STARTED", now, request.deviceId()));
        return response(session, now);
    }

    @Transactional
    public SessionResponse active(UUID userId) {
        Instant now = Instant.now(clock);
        FocusSession session = sessions.findActive(userId)
                .orElseThrow(() -> new NotFoundException("active_session_not_found", "No active session."));
        if (session.reconcile(now)) {
            sessions.save(session);
            events.save(new SessionEvent(session, "COMPLETED", now, "server"));
        }
        return response(session, now);
    }

    @Transactional(readOnly = true)
    public SessionResponse get(UUID userId, String id) {
        FocusSession session = sessions.findOwned(id, userId)
                .orElseThrow(() -> new NotFoundException("session_not_found", "Session not found."));
        return response(session, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public SessionPage list(UUID userId, String cursor, int requestedLimit) {
        int limit = Math.min(100, Math.max(1, requestedLimit));
        Instant now = Instant.now(clock);
        Cursor decoded = decode(cursor);
        var page = decoded == null
                ? sessions.findHistory(userId, PageRequest.of(0, limit + 1))
                : sessions.findHistoryAfter(userId, decoded.startedAt, decoded.id,
                        PageRequest.of(0, limit + 1));
        boolean hasMore = page.size() > limit;
        var values = hasMore ? page.subList(0, limit) : page;
        String next = hasMore ? encode(values.get(values.size() - 1)) : null;
        return new SessionPage(values.stream().map(value -> response(value, now)).toList(), next);
    }

    @Transactional
    public SessionResponse pause(UUID userId, String id, TransitionRequest request) {
        return transition(userId, id, request, "PAUSED", FocusSession::pause);
    }

    @Transactional
    public SessionResponse resume(UUID userId, String id, TransitionRequest request) {
        return transition(userId, id, request, "RESUMED", FocusSession::resume);
    }

    @Transactional
    public SessionResponse abandon(UUID userId, String id, TransitionRequest request) {
        return transition(userId, id, request, "ABANDONED", FocusSession::abandon);
    }

    @Transactional
    public void reconcileOne(String id) {
        Instant now = Instant.now(clock);
        sessions.findByIdForUpdate(id).ifPresent(session -> {
            if (session.reconcile(now)) {
                sessions.save(session);
                events.save(new SessionEvent(session, "COMPLETED", now, "server"));
            }
        });
    }

    private SessionResponse transition(UUID userId, String id, TransitionRequest request,
                                       String requestedEvent,
                                       BiFunction<FocusSession, Instant, Boolean> operation) {
        FocusSession session = sessions.findOwnedForUpdate(id, userId)
                .orElseThrow(() -> new NotFoundException("session_not_found", "Session not found."));
        if (request.version() != null && request.version() != session.getVersion()) {
            throw new ConflictException("stale_version", "Session changed on another device.");
        }
        SessionStatus before = session.getStatus();
        Instant now = Instant.now(clock);
        boolean requestedChange = operation.apply(session, now);
        if (session.getStatus() != before) {
            String event = session.getStatus() == SessionStatus.COMPLETED ? "COMPLETED" : requestedEvent;
            sessions.save(session);
            events.save(new SessionEvent(session, event, now, request.deviceId()));
        } else if (!requestedChange && !idempotentState(requestedEvent, session.getStatus())) {
            throw new ConflictException("invalid_session_transition",
                    "This transition is not valid from " + session.getStatus() + '.');
        }
        return response(session, now);
    }

    private boolean idempotentState(String event, SessionStatus status) {
        return (event.equals("PAUSED") && status == SessionStatus.PAUSED)
                || (event.equals("RESUMED") && status == SessionStatus.RUNNING)
                || (event.equals("ABANDONED") && status == SessionStatus.ABANDONED)
                || status == SessionStatus.COMPLETED;
    }

    private SessionResponse response(FocusSession session, Instant now) {
        return new SessionResponse(session.getId(),
                session.getTask() == null ? null : session.getTask().getId(),
                session.getTaskTitleSnapshot(), session.getStatus(), session.getPlannedDurationSeconds(),
                session.getActualFocusSeconds(), session.secondsRemainingAt(now), session.getStartedAt(),
                session.getCurrentDeadlineAt(), session.getEndedAt(), session.getSessionTimezone(),
                session.getLocalDate(), session.getVersion());
    }

    private String encode(FocusSession session) {
        String value = session.getStartedAt().toEpochMilli() + ":" + session.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decode(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            return new Cursor(Instant.ofEpochMilli(Long.parseLong(parts[0])), parts[1]);
        } catch (RuntimeException exception) {
            throw new com.vesta.api.common.error.BadRequestException("invalid_cursor", "Cursor is invalid.");
        }
    }

    private record Cursor(Instant startedAt, String id) {
    }
}
