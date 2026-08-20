package com.vesta.api.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vesta.security")
public record SecurityProperties(
        String jwtSecret,
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        boolean cookieSecure,
        boolean requireHttps) {
}
