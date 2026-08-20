package com.vesta.api.auth;

import com.vesta.api.auth.AccountToken.Purpose;
import com.vesta.api.auth.AuthDtos.AuthResponse;
import com.vesta.api.auth.AuthDtos.LoginRequest;
import com.vesta.api.auth.AuthDtos.RegisterRequest;
import com.vesta.api.common.config.SecurityProperties;
import com.vesta.api.common.error.BadRequestException;
import com.vesta.api.common.error.ConflictException;
import com.vesta.api.preferences.UserPreferences;
import com.vesta.api.preferences.UserPreferencesRepository;
import com.vesta.api.user.UserAccount;
import com.vesta.api.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final RefreshSessionRepository refreshSessions;
    private final AccountTokenRepository accountTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final SecurityProperties securityProperties;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthService(UserRepository users, UserPreferencesRepository preferences,
                       RefreshSessionRepository refreshSessions, AccountTokenRepository accountTokens,
                       PasswordEncoder passwordEncoder, JwtService jwtService, MailService mailService,
                       SecurityProperties securityProperties, Clock clock) {
        this.users = users;
        this.preferences = preferences;
        this.refreshSessions = refreshSessions;
        this.accountTokens = accountTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.securityProperties = securityProperties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("vesta-dummy-password-not-used");
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validatePassword(request.password());
        validateTimezone(request.timezone());
        if (users.findActiveByEmail(email).isPresent()) {
            throw new ConflictException("email_already_registered", "An account already uses this email.");
        }
        Instant now = Instant.now(clock);
        UserAccount user = users.save(new UserAccount(UUID.randomUUID(), email,
                passwordEncoder.encode(request.password()), clean(request.displayName()),
                request.timezone(), request.locale() == null ? "en-US" : request.locale(), now));
        preferences.save(new UserPreferences(user, now));
        String verificationToken = createAccountToken(user, Purpose.VERIFY_EMAIL, Duration.ofHours(24), now);
        mailService.sendVerification(user.getEmail(), verificationToken);
        return issueTokens(user, request.deviceName(), now);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        UserAccount user = users.findActiveByEmail(normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issueTokens(user, request.deviceName(), Instant.now(clock));
    }

    @Transactional
    public AuthResult refresh(String rawToken, String deviceName) {
        Instant now = Instant.now(clock);
        RefreshSession current = refreshSessions.findByHashForUpdate(TokenHash.sha256(rawToken))
                .orElseThrow(this::invalidRefreshToken);
        if (!current.isUsableAt(now)) {
            throw invalidRefreshToken();
        }
        current.revoke(now);
        return issueTokens(current.getUser(), deviceName, now);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshSessions.findByHashForUpdate(TokenHash.sha256(rawToken))
                .ifPresent(value -> value.revoke(Instant.now(clock)));
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshSessions.revokeAll(userId, Instant.now(clock));
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        Instant now = Instant.now(clock);
        AccountToken token = accountTokens.findByHashForUpdate(TokenHash.sha256(rawToken))
                .orElseThrow(() -> invalidAccountToken("verification_token_invalid"));
        if (token.getPurpose() != Purpose.VERIFY_EMAIL || !token.isUsableAt(now)) {
            throw invalidAccountToken("verification_token_invalid");
        }
        token.use(now);
        token.getUser().verifyEmail(now);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        users.findActiveByEmail(normalizeEmail(email)).ifPresent(user -> {
            Instant now = Instant.now(clock);
            String token = createAccountToken(user, Purpose.RESET_PASSWORD, Duration.ofMinutes(30), now);
            mailService.sendPasswordReset(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validatePassword(newPassword);
        Instant now = Instant.now(clock);
        AccountToken token = accountTokens.findByHashForUpdate(TokenHash.sha256(rawToken))
                .orElseThrow(() -> invalidAccountToken("reset_token_invalid"));
        if (token.getPurpose() != Purpose.RESET_PASSWORD || !token.isUsableAt(now)) {
            throw invalidAccountToken("reset_token_invalid");
        }
        token.use(now);
        token.getUser().changePassword(passwordEncoder.encode(newPassword), now);
        refreshSessions.revokeAll(token.getUser().getId(), now);
    }

    private AuthResult issueTokens(UserAccount user, String deviceName, Instant now) {
        String refreshToken = TokenHash.randomToken();
        refreshSessions.save(new RefreshSession(UUID.randomUUID(), user, TokenHash.sha256(refreshToken),
                clean(deviceName), now, now.plus(securityProperties.refreshTokenTtl())));
        AuthResponse response = new AuthResponse(jwtService.issue(user), "Bearer",
                securityProperties.accessTokenTtl().toSeconds(), AuthDtos.UserResponse.from(user));
        return new AuthResult(response, refreshToken, TokenHash.randomToken());
    }

    private String createAccountToken(UserAccount user, Purpose purpose, Duration ttl, Instant now) {
        String raw = TokenHash.randomToken();
        accountTokens.save(new AccountToken(UUID.randomUUID(), user, TokenHash.sha256(raw), purpose,
                now.plus(ttl), now));
        return raw;
    }

    private void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("password_too_long", "Password must be at most 72 UTF-8 bytes.");
        }
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (ZoneRulesException exception) {
            throw new BadRequestException("invalid_timezone", "Use a valid IANA timezone.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BadRequestException invalidCredentials() {
        return new BadRequestException("invalid_credentials", "Email or password is invalid.");
    }

    private BadRequestException invalidRefreshToken() {
        return new BadRequestException("refresh_token_invalid", "Refresh session is invalid or expired.");
    }

    private BadRequestException invalidAccountToken(String code) {
        return new BadRequestException(code, "Token is invalid or expired.");
    }

    public record AuthResult(AuthResponse response, String refreshToken, String csrfToken) {
    }
}
