package com.vesta.api.auth;

import com.vesta.api.user.UserAccount;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 10, max = 72) String password,
            @Size(max = 80) String displayName,
            @NotBlank @Size(max = 64) String timezone,
            @Pattern(regexp = "^[A-Za-z]{2,3}([_-][A-Za-z0-9]{2,8})?$") String locale,
            @Size(max = 160) String deviceName) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 72) String password,
            @Size(max = 160) String deviceName) {
    }

    public record TokenRequest(@NotBlank String token) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 320) String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 10, max = 72) String newPassword) {
    }

    public record UserResponse(UUID id, String email, String displayName, String timezone,
                               String locale, boolean emailVerified, long version) {
        public static UserResponse from(UserAccount user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                    user.getTimezone(), user.getLocale(), user.getEmailVerifiedAt() != null,
                    user.getVersion());
        }
    }

    public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds,
                               UserResponse user) {
    }

    public record MessageResponse(String message) {
    }
}

