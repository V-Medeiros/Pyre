package com.vesta.api.importer;

import com.vesta.api.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_batches")
public class ImportBatch {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;
    @Column(name = "imported_tasks", nullable = false)
    private int importedTasks;
    @Column(name = "imported_sessions", nullable = false)
    private int importedSessions;
    @Column(name = "ignored_items", nullable = false)
    private int ignoredItems;
    @Column(name = "invalid_items", nullable = false)
    private int invalidItems;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ImportBatch() {
    }

    public ImportBatch(UserAccount user, String key, int importedTasks, int importedSessions,
                       int ignoredItems, int invalidItems, Instant now) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.idempotencyKey = key;
        this.importedTasks = importedTasks;
        this.importedSessions = importedSessions;
        this.ignoredItems = ignoredItems;
        this.invalidItems = invalidItems;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public int getImportedTasks() { return importedTasks; }
    public int getImportedSessions() { return importedSessions; }
    public int getIgnoredItems() { return ignoredItems; }
    public int getInvalidItems() { return invalidItems; }
    public Instant getCreatedAt() { return createdAt; }
}

