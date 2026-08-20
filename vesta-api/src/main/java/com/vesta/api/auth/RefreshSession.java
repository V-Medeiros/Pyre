package com.vesta.api.auth;

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
@Table(name = "refresh_sessions")
public class RefreshSession {

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "device_name", length = 160)
    private String deviceName;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshSession() {
    }

    public RefreshSession(UUID id, UserAccount user, String tokenHash, String deviceName,
                          Instant now, Instant expiresAt) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.deviceName = deviceName;
        this.expiresAt = expiresAt;
        this.lastUsedAt = now;
        this.createdAt = now;
    }

    public UserAccount getUser() { return user; }
    public Instant getRevokedAt() { return revokedAt; }

    public boolean isUsableAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now) && user.getDeletedAt() == null;
    }

    public void revoke(Instant now) {
        revokedAt = now;
        lastUsedAt = now;
    }
}

