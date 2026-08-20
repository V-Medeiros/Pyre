package com.vesta.api.session;

import com.vesta.api.task.Task;
import com.vesta.api.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Entity
@Table(name = "focus_sessions")
public class FocusSession {

    @Id
    @Column(length = 100)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;
    @Column(name = "task_title_snapshot", length = 120)
    private String taskTitleSnapshot;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SessionStatus status;
    @Column(name = "planned_duration_seconds", nullable = false)
    private int plannedDurationSeconds;
    @Column(name = "accumulated_focus_seconds", nullable = false)
    private int accumulatedFocusSeconds;
    @Column(name = "actual_focus_seconds")
    private Integer actualFocusSeconds;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "state_changed_at", nullable = false)
    private Instant stateChangedAt;
    @Column(name = "current_deadline_at")
    private Instant currentDeadlineAt;
    @Column(name = "ended_at")
    private Instant endedAt;
    @Column(name = "session_timezone", nullable = false, length = 64)
    private String sessionTimezone;
    @Column(name = "local_date")
    private LocalDate localDate;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;

    protected FocusSession() {
    }

    public static FocusSession start(String id, UserAccount user, Task task,
                                     int plannedSeconds, Instant now) {
        FocusSession session = new FocusSession();
        session.id = id;
        session.user = user;
        session.task = task;
        session.taskTitleSnapshot = task == null ? null : task.getTitle();
        session.status = SessionStatus.RUNNING;
        session.plannedDurationSeconds = plannedSeconds;
        session.accumulatedFocusSeconds = 0;
        session.startedAt = now;
        session.stateChangedAt = now;
        session.currentDeadlineAt = now.plusSeconds(plannedSeconds);
        session.sessionTimezone = user.getTimezone();
        session.createdAt = now;
        session.updatedAt = now;
        return session;
    }

    public static FocusSession imported(String id, UserAccount user, Task task, String titleSnapshot,
                                        SessionStatus status, int plannedSeconds, int actualSeconds,
                                        Instant startedAt, Instant endedAt, String timezone) {
        FocusSession session = new FocusSession();
        session.id = id;
        session.user = user;
        session.task = task;
        session.taskTitleSnapshot = titleSnapshot;
        session.status = status;
        session.plannedDurationSeconds = plannedSeconds;
        session.accumulatedFocusSeconds = actualSeconds;
        session.actualFocusSeconds = actualSeconds;
        session.startedAt = startedAt;
        session.stateChangedAt = endedAt;
        session.endedAt = endedAt;
        session.sessionTimezone = timezone;
        session.localDate = endedAt.atZone(ZoneId.of(timezone)).toLocalDate();
        session.createdAt = startedAt;
        session.updatedAt = endedAt;
        return session;
    }

    public static FocusSession importedActive(String id, UserAccount user, Task task,
                                               int plannedSeconds, SessionStatus status,
                                               Instant startedAt, Instant deadline,
                                               Integer pausedSecondsRemaining, Instant now) {
        FocusSession session = new FocusSession();
        session.id = id;
        session.user = user;
        session.task = task;
        session.taskTitleSnapshot = task == null ? null : task.getTitle();
        session.status = status;
        session.plannedDurationSeconds = plannedSeconds;
        int remaining = pausedSecondsRemaining == null
                ? plannedSeconds
                : Math.max(1, Math.min(plannedSeconds, pausedSecondsRemaining));
        session.accumulatedFocusSeconds = plannedSeconds - remaining;
        session.startedAt = startedAt;
        session.stateChangedAt = now;
        session.currentDeadlineAt = status == SessionStatus.RUNNING
                ? (deadline == null || !deadline.isAfter(now) ? now.plusSeconds(remaining) : deadline)
                : null;
        session.sessionTimezone = user.getTimezone();
        session.createdAt = startedAt;
        session.updatedAt = now;
        return session;
    }

    public boolean reconcile(Instant now) {
        if (status == SessionStatus.RUNNING && !currentDeadlineAt.isAfter(now)) {
            completeAt(currentDeadlineAt);
            return true;
        }
        return false;
    }

    public boolean pause(Instant now) {
        if (reconcile(now) || status != SessionStatus.RUNNING) return false;
        accumulatedFocusSeconds = Math.min(plannedDurationSeconds,
                accumulatedFocusSeconds + elapsedSinceChange(now));
        status = SessionStatus.PAUSED;
        stateChangedAt = now;
        currentDeadlineAt = null;
        updatedAt = now;
        return true;
    }

    public boolean resume(Instant now) {
        if (status != SessionStatus.PAUSED) return false;
        int remaining = Math.max(1, plannedDurationSeconds - accumulatedFocusSeconds);
        status = SessionStatus.RUNNING;
        stateChangedAt = now;
        currentDeadlineAt = now.plusSeconds(remaining);
        updatedAt = now;
        return true;
    }

    public boolean abandon(Instant now) {
        if (reconcile(now) || status == SessionStatus.COMPLETED) return false;
        if (status == SessionStatus.ABANDONED) return false;
        if (status == SessionStatus.RUNNING) {
            accumulatedFocusSeconds = Math.min(plannedDurationSeconds,
                    accumulatedFocusSeconds + elapsedSinceChange(now));
        }
        status = SessionStatus.ABANDONED;
        actualFocusSeconds = accumulatedFocusSeconds;
        endedAt = now;
        stateChangedAt = now;
        currentDeadlineAt = null;
        localDate = now.atZone(ZoneId.of(sessionTimezone)).toLocalDate();
        updatedAt = now;
        return true;
    }

    private void completeAt(Instant completion) {
        status = SessionStatus.COMPLETED;
        accumulatedFocusSeconds = plannedDurationSeconds;
        actualFocusSeconds = plannedDurationSeconds;
        endedAt = completion;
        stateChangedAt = completion;
        currentDeadlineAt = null;
        localDate = completion.atZone(ZoneId.of(sessionTimezone)).toLocalDate();
        updatedAt = completion;
    }

    public int secondsRemainingAt(Instant now) {
        if (status == SessionStatus.PAUSED) {
            return Math.max(0, plannedDurationSeconds - accumulatedFocusSeconds);
        }
        if (status == SessionStatus.RUNNING) {
            return Math.max(0, (int) Math.ceil(Duration.between(now, currentDeadlineAt).toMillis() / 1000.0));
        }
        return 0;
    }

    private int elapsedSinceChange(Instant now) {
        return Math.max(0, (int) Duration.between(stateChangedAt, now).toSeconds());
    }

    public String getId() { return id; }
    public UserAccount getUser() { return user; }
    public Task getTask() { return task; }
    public String getTaskTitleSnapshot() { return taskTitleSnapshot; }
    public SessionStatus getStatus() { return status; }
    public int getPlannedDurationSeconds() { return plannedDurationSeconds; }
    public int getAccumulatedFocusSeconds() { return accumulatedFocusSeconds; }
    public Integer getActualFocusSeconds() { return actualFocusSeconds; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCurrentDeadlineAt() { return currentDeadlineAt; }
    public Instant getEndedAt() { return endedAt; }
    public String getSessionTimezone() { return sessionTimezone; }
    public LocalDate getLocalDate() { return localDate; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version == null ? 0 : version; }
}
