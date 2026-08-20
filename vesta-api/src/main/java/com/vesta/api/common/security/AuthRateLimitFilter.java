package com.vesta.api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int LIMIT = 20;
    private static final long WINDOW_SECONDS = 60;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public AuthRateLimitFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"POST".equals(request.getMethod()) || !path.startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long now = Instant.now(clock).getEpochSecond();
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_SECONDS);
        }
        String key = clientAddress(request) + ':' + request.getRequestURI();
        Window window = windows.compute(key, (ignored, current) ->
                current == null || now - current.startedAt >= WINDOW_SECONDS
                        ? new Window(now, 1)
                        : new Window(current.startedAt, current.count + 1));
        if (window.count > LIMIT) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"type\":\"https://vesta.app/problems/rate_limited\","
                    + "\"title\":\"Too Many Requests\",\"status\":429,"
                    + "\"detail\":\"Try again in a minute.\",\"code\":\"rate_limited\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientAddress(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private record Window(long startedAt, int count) {
    }
}
