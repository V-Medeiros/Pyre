package com.vesta.api.preferences;

import com.vesta.api.common.error.ConflictException;
import com.vesta.api.common.error.NotFoundException;
import com.vesta.api.preferences.PreferencesDtos.PreferencesResponse;
import com.vesta.api.preferences.PreferencesDtos.UpdatePreferencesRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferencesService {

    private final UserPreferencesRepository repository;
    private final Clock clock;

    public PreferencesService(UserPreferencesRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PreferencesResponse get(UUID userId) {
        return PreferencesResponse.from(find(userId));
    }

    @Transactional
    public PreferencesResponse update(UUID userId, UpdatePreferencesRequest request) {
        UserPreferences preferences = find(userId);
        if (request.version() != null && request.version() != preferences.getVersion()) {
            throw new ConflictException("stale_version", "Preferences changed on another device.");
        }
        preferences.update(request.defaultDurationMinutes(), request.soundEnabled(), request.theme(),
                Instant.now(clock));
        return PreferencesResponse.from(repository.save(preferences));
    }

    private UserPreferences find(UUID userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("preferences_not_found", "Preferences not found."));
    }
}

