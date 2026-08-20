package com.vesta.api.auth;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_tokens")
public class AccountToken {

    public enum Purpose { VERIFY_EMAIL, RESET_PASSWORD }

    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Purpose purpose;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountToken() {
    }

    public AccountToken(UUID id, UserAccount user, String tokenHash, Purpose purpose,
                        Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public UserAccount getUser() { return user; }
    public Purpose getPurpose() { return purpose; }

    public boolean isUsableAt(Instant now) {
        return usedAt == null && expiresAt.isAfter(now) && user.getDeletedAt() == null;
    }

    public void use(Instant now) {
        usedAt = now;
    }
}

