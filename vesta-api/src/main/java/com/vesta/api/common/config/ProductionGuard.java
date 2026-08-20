package com.vesta.api.common.config;

import java.util.Arrays;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionGuard implements ApplicationRunner {

    private static final String LOCAL_SECRET = "local-development-secret-change-before-production-123456";
    private final Environment environment;
    private final SecurityProperties security;
    private final CorsProperties cors;

    public ProductionGuard(Environment environment, SecurityProperties security, CorsProperties cors) {
        this.environment = environment;
        this.security = security;
        this.cors = cors;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("production");
        if (!production) return;
        if (LOCAL_SECRET.equals(security.jwtSecret()) || !security.cookieSecure() || !security.requireHttps()) {
            throw new IllegalStateException("Production requires a unique JWT secret, secure cookies, and HTTPS");
        }
        if (cors.origins().stream().anyMatch(origin -> origin.contains("localhost"))) {
            throw new IllegalStateException("Production CORS must not include localhost");
        }
    }
}
