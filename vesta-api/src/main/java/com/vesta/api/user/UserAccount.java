package com.vesta.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    private UUID id;
    @Column(nullable = false, length = 320)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "display_name", length = 80)
    private String displayName;
    @Column(nullable = false, length = 64)
    private String timezone;
    @Column(nullable = false, length = 16)
    private String locale;
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    @Version
    private Long version;

    protected UserAccount() {
    }

    public UserAccount(UUID id, String email, String passwordHash, String displayName,
                       String timezone, String locale, Instant now) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.timezone = timezone;
        this.locale = locale;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getTimezone() { return timezone; }
    public String getLocale() { return locale; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public long getVersion() { return version == null ? 0 : version; }

    public void verifyEmail(Instant now) {
        emailVerifiedAt = now;
        updatedAt = now;
    }

    public void changePassword(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        this.updatedAt = now;
    }

    public void updateProfile(String displayName, String timezone, String locale, Instant now) {
        this.displayName = displayName;
        this.timezone = timezone;
        this.locale = locale;
        this.updatedAt = now;
    }

    public void scheduleDeletion(Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
    }
}
