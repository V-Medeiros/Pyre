package com.vesta.api.user;

import com.vesta.api.auth.AuthDtos.UserResponse;
import com.vesta.api.sync.SyncDtos.SyncResponse;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record UpdateProfileRequest(
            @Size(max = 80) String displayName,
            @Size(max = 64) String timezone,
            @Pattern(regexp = "^[A-Za-z]{2,3}([_-][A-Za-z0-9]{2,8})?$") String locale,
            @PositiveOrZero Long version) {
    }

    public record AccountExport(Instant exportedAt, UserResponse user, SyncResponse data) {
    }
}

