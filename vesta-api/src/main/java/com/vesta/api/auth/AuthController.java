package com.vesta.api.auth;

import com.vesta.api.auth.AuthDtos.AuthResponse;
import com.vesta.api.auth.AuthDtos.ForgotPasswordRequest;
import com.vesta.api.auth.AuthDtos.LoginRequest;
import com.vesta.api.auth.AuthDtos.MessageResponse;
import com.vesta.api.auth.AuthDtos.RegisterRequest;
import com.vesta.api.auth.AuthDtos.ResetPasswordRequest;
import com.vesta.api.auth.AuthDtos.TokenRequest;
import com.vesta.api.common.config.SecurityProperties;
import com.vesta.api.common.error.BadRequestException;
import com.vesta.api.common.security.CurrentUser;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "vesta_refresh";
    private static final String CSRF_COOKIE = "vesta_csrf";
    private final AuthService service;
    private final SecurityProperties properties;

    public AuthController(AuthService service, SecurityProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                          HttpServletResponse response) {
        return withCookies(service.register(request), response);
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletResponse response) {
        return withCookies(service.login(request), response);
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE) String refreshToken,
            @CookieValue(name = CSRF_COOKIE) String csrfCookie,
            @RequestHeader("X-CSRF-Token") String csrfHeader,
            @RequestHeader(name = "User-Agent", required = false) String deviceName,
            HttpServletResponse response) {
        validateCsrf(csrfCookie, csrfHeader);
        return withCookies(service.refresh(refreshToken, deviceName), response);
    }

    @PostMapping("/logout")
    MessageResponse logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-CSRF-Token", required = false) String csrfHeader,
            HttpServletResponse response) {
        if (refreshToken != null) validateCsrf(csrfCookie, csrfHeader);
        service.logout(refreshToken);
        clearCookies(response);
        return new MessageResponse("Logged out.");
    }

    @PostMapping("/logout-all")
    MessageResponse logoutAll(Authentication authentication, HttpServletResponse response) {
        service.logoutAll(CurrentUser.id(authentication));
        clearCookies(response);
        return new MessageResponse("All devices were logged out.");
    }

    @PostMapping("/verify-email")
    MessageResponse verifyEmail(@Valid @RequestBody TokenRequest request) {
        service.verifyEmail(request.token());
        return new MessageResponse("Email verified.");
    }

    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        service.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(request.token(), request.newPassword());
        return new MessageResponse("Password changed. Sign in again.");
    }

    private ResponseEntity<AuthResponse> withCookies(AuthService.AuthResult result,
                                                     HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, result.refreshToken(), true,
                properties.refreshTokenTtl()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE, result.csrfToken(), false,
                properties.refreshTokenTtl()).toString());
        return ResponseEntity.ok(result.response());
    }

    private ResponseCookie cookie(String name, String value, boolean httpOnly, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(httpOnly ? "/api/v1/auth" : "/")
                .maxAge(maxAge)
                .build();
    }

    private void clearCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", true, Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE, "", false, Duration.ZERO).toString());
    }

    private void validateCsrf(String cookie, String header) {
        if (cookie == null || header == null || !MessageDigest.isEqual(
                cookie.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8))) {
            throw new BadRequestException("csrf_invalid", "Refresh CSRF token is missing or invalid.");
        }
    }
}
