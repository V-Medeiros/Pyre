package com.vesta.api.preferences;

import com.vesta.api.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "default_duration_minutes", nullable = false)
    private int defaultDurationMinutes;
    @Column(name = "sound_enabled", nullable = false)
    private boolean soundEnabled;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Theme theme;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;

    protected UserPreferences() {
    }

    public UserPreferences(UserAccount user, Instant now) {
        this.userId = user.getId();
        this.defaultDurationMinutes = 25;
        this.soundEnabled = true;
        this.theme = Theme.SYSTEM;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getUserId() { return userId; }
    public int getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public boolean isSoundEnabled() { return soundEnabled; }
    public Theme getTheme() { return theme; }
    public long getVersion() { return version == null ? 0 : version; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(Integer duration, Boolean sound, Theme nextTheme, Instant now) {
        if (duration != null) defaultDurationMinutes = duration;
        if (sound != null) soundEnabled = sound;
        if (nextTheme != null) theme = nextTheme;
        updatedAt = now;
    }
}
