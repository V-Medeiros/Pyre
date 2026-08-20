package com.vesta.api.preferences;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public final class PreferencesDtos {

    private PreferencesDtos() {
    }

    public record UpdatePreferencesRequest(
            @Min(5) @Max(120) Integer defaultDurationMinutes,
            Boolean soundEnabled,
            Theme theme,
            @PositiveOrZero Long version) {
    }

    public record PreferencesResponse(
            int defaultDurationMinutes,
            boolean soundEnabled,
            Theme theme,
            long version) {
        public static PreferencesResponse from(UserPreferences value) {
            return new PreferencesResponse(value.getDefaultDurationMinutes(), value.isSoundEnabled(),
                    value.getTheme(), value.getVersion());
        }
    }
}

