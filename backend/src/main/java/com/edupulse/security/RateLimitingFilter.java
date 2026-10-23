package com.edupulse.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Instance-local sliding-window rate limiting filter for authentication endpoints.
 * Protects against brute-force password guessing and token request flooding.
 * 
 * Note: This rate limiter is instance-local (in-memory). In a multi-instance horizontally
 * scaled deployment without sticky sessions, distributed rate limiting with Redis is recommended.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int LOGIN_MAX_REQUESTS_PER_MINUTE = 10;
    private static final int FORGOT_PASSWORD_MAX_REQUESTS_PER_MINUTE = 5;
    private static final int RESET_PASSWORD_MAX_REQUESTS_PER_MINUTE = 5;
    private static final long WINDOW_MS = 60_000L; // 1 minute

    private final Map<String, ConcurrentLinkedQueue<Long>> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method)) {
            int limit = 0;
            String endpointKey = "";

            if (path.endsWith("/api/auth/login")) {
                limit = LOGIN_MAX_REQUESTS_PER_MINUTE;
                endpointKey = "login";
            } else if (path.endsWith("/api/auth/forgot-password")) {
                limit = FORGOT_PASSWORD_MAX_REQUESTS_PER_MINUTE;
                endpointKey = "forgot";
            } else if (path.endsWith("/api/auth/reset-password")) {
                limit = RESET_PASSWORD_MAX_REQUESTS_PER_MINUTE;
                endpointKey = "reset";
            }

            if (limit > 0) {
                String clientIp = getClientIp(request);
                int effectiveLimit = isLoopbackAddress(clientIp) ? 100 : limit;
                String rateKey = endpointKey + ":" + clientIp;

                if (!isAllowed(rateKey, effectiveLimit)) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.setHeader("Retry-After", "60");
                    response.getWriter().write("{\"message\": \"Too many requests. Please try again later.\"}");
                    response.getWriter().flush();
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoopbackAddress(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equalsIgnoreCase(ip);
    }

    private boolean isAllowed(String key, int maxRequests) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedQueue<Long> timestamps = requestCounts.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        // Evict timestamps older than 1 minute
        while (!timestamps.isEmpty() && (now - timestamps.peek()) > WINDOW_MS) {
            timestamps.poll();
        }

        if (timestamps.size() >= maxRequests) {
            return false;
        }

        timestamps.add(now);

        // Periodically guard against map growth
        if (requestCounts.size() > 5000) {
            cleanupExpiredKeys(now);
        }

        return true;
    }

    private void cleanupExpiredKeys(long now) {
        requestCounts.entrySet().removeIf(entry -> {
            ConcurrentLinkedQueue<Long> queue = entry.getValue();
            while (!queue.isEmpty() && (now - queue.peek()) > WINDOW_MS) {
                queue.poll();
            }
            return queue.isEmpty();
        });
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
