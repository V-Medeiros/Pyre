package com.vesta.api.task;

import com.vesta.api.common.error.BadRequestException;
import com.vesta.api.common.error.ConflictException;
import com.vesta.api.common.error.NotFoundException;
import com.vesta.api.task.TaskDtos.CreateTaskRequest;
import com.vesta.api.task.TaskDtos.TaskFilter;
import com.vesta.api.task.TaskDtos.TaskPage;
import com.vesta.api.task.TaskDtos.TaskResponse;
import com.vesta.api.task.TaskDtos.UpdateTaskRequest;
import com.vesta.api.task.TaskDtos.VersionRequest;
import com.vesta.api.user.UserAccount;
import com.vesta.api.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final UserRepository users;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public TaskService(TaskRepository tasks, UserRepository users,
                       NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.tasks = tasks;
        this.users = users;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public TaskResponse create(UUID userId, CreateTaskRequest request) {
        UserAccount user = users.findActiveById(userId)
                .orElseThrow(() -> new NotFoundException("user_not_found", "User not found."));
        String id = request.id() == null ? "task_" + UUID.randomUUID() : request.id();
        Task existing = tasks.findById(id).orElse(null);
        if (existing != null) {
            if (existing.getUser().getId().equals(userId) && existing.getDeletedAt() == null
                    && existing.getTitle().equals(request.title().trim())) {
                return response(existing);
            }
            throw new ConflictException("task_id_conflict", "Task identifier is already in use.");
        }
        Task task = tasks.save(new Task(id, user, request.title().trim(), Instant.now(clock)));
        return response(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID userId, String id) {
        return response(findActive(userId, id));
    }

    @Transactional(readOnly = true)
    public TaskPage list(UUID userId, TaskFilter filter, String cursor, int requestedLimit) {
        int limit = Math.min(100, Math.max(1, requestedLimit));
        Cursor decoded = decode(cursor);
        List<Task> filtered = tasks.findAllByUserIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(userId)
                .stream()
                .filter(task -> filter == TaskFilter.ALL
                        || (filter == TaskFilter.OPEN && task.getCompletedAt() == null)
                        || (filter == TaskFilter.COMPLETED && task.getCompletedAt() != null))
                .filter(task -> decoded == null || task.getUpdatedAt().isBefore(decoded.updatedAt)
                        || (task.getUpdatedAt().equals(decoded.updatedAt)
                        && task.getId().compareTo(decoded.id) < 0))
                .limit(limit + 1L)
                .toList();
        boolean hasMore = filtered.size() > limit;
        List<Task> page = hasMore ? filtered.subList(0, limit) : filtered;
        String next = hasMore ? encode(page.get(page.size() - 1)) : null;
        return new TaskPage(page.stream().map(this::response).toList(), next);
    }

    @Transactional
    public TaskResponse rename(UUID userId, String id, UpdateTaskRequest request) {
        Task task = findActive(userId, id);
        checkVersion(task, request.version());
        task.rename(request.title().trim(), Instant.now(clock));
        return response(tasks.save(task));
    }

    @Transactional
    public TaskResponse complete(UUID userId, String id, VersionRequest request) {
        Task task = findActive(userId, id);
        rejectWhenActive(userId, id);
        checkVersion(task, request.version());
        task.complete(Instant.now(clock));
        return response(tasks.save(task));
    }

    @Transactional
    public TaskResponse reopen(UUID userId, String id, VersionRequest request) {
        Task task = findActive(userId, id);
        checkVersion(task, request.version());
        task.reopen(Instant.now(clock));
        return response(tasks.save(task));
    }

    @Transactional
    public void delete(UUID userId, String id, Long version) {
        Task task = tasks.findOwnedIncludingDeleted(id, userId)
                .orElseThrow(() -> new NotFoundException("task_not_found", "Task not found."));
        if (task.getDeletedAt() != null) return;
        rejectWhenActive(userId, id);
        checkVersion(task, version);
        task.delete(Instant.now(clock));
    }

    private void rejectWhenActive(UUID userId, String id) {
        if (tasks.hasActiveSession(userId, id)) {
            throw new ConflictException("task_in_active_session",
                    "A task used by the active session cannot be changed.");
        }
    }

    private Task findActive(UUID userId, String id) {
        return tasks.findActiveOwned(id, userId)
                .orElseThrow(() -> new NotFoundException("task_not_found", "Task not found."));
    }

    private void checkVersion(Task task, Long expected) {
        if (expected != null && expected != task.getVersion()) {
            throw new ConflictException("stale_version", "Task changed on another device.");
        }
    }

    private TaskResponse response(Task task) {
        Long count = jdbc.queryForObject("select count(*) from focus_sessions where user_id = :userId "
                        + "and task_id = :taskId and status = 'COMPLETED'",
                new MapSqlParameterSource().addValue("userId", task.getUser().getId())
                        .addValue("taskId", task.getId()), Long.class);
        return new TaskResponse(task.getId(), task.getTitle(), task.getCompletedAt() != null,
                count == null ? 0 : count, task.getCreatedAt(), task.getUpdatedAt(), task.getVersion());
    }

    private String encode(Task task) {
        String value = task.getUpdatedAt().toEpochMilli() + ":" + task.getId();
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
            throw new BadRequestException("invalid_cursor", "Cursor is invalid.");
        }
    }

    private record Cursor(Instant updatedAt, String id) {
    }
}

