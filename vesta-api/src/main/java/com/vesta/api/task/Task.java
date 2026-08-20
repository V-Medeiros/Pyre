package com.vesta.api.task;

import com.vesta.api.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @Column(length = 100)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @Version
    private Long version;

    protected Task() {
    }

    public Task(String id, UserAccount user, String title, Instant now) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Task imported(String id, UserAccount user, String title, boolean completed,
                                Instant createdAt, Instant updatedAt) {
        Task task = new Task(id, user, title, createdAt);
        task.updatedAt = updatedAt.isBefore(createdAt) ? createdAt : updatedAt;
        task.completedAt = completed ? task.updatedAt : null;
        return task;
    }

    public String getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getTitle() { return title; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public long getVersion() { return version == null ? 0 : version; }

    public void rename(String title, Instant now) {
        this.title = title;
        this.updatedAt = now;
    }

    public void complete(Instant now) {
        if (completedAt == null) {
            completedAt = now;
            updatedAt = now;
        }
    }

    public void reopen(Instant now) {
        if (completedAt != null) {
            completedAt = null;
            updatedAt = now;
        }
    }

    public void delete(Instant now) {
        if (deletedAt == null) {
            deletedAt = now;
            updatedAt = now;
        }
    }
}
