package com.ghostcoach.filter;

import com.ghostcoach.security.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Runs first on every request to establish telemetry context in MDC.
 *
 * Sets two MDC keys that appear in every subsequent log line for this thread:
 *   - requestId: short UUID identifying this specific HTTP request
 *   - userId:    first 8 hex chars of SHA-256(email) — correlatable but not reversible
 *
 * Also adds X-Request-Id to the response header so clients can include it in bug reports.
 * MDC is always cleared in the finally block to prevent context leakage across thread reuse.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    static final String REQUEST_ID = "requestId";
    static final String USER_ID = "userId";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MDC.put(REQUEST_ID, requestId);
        MDC.put(USER_ID, resolveUserId(request));

        response.setHeader("X-Request-Id", requestId);

        long start = System.currentTimeMillis();
        log.info("→ {} {} [ip={}]", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());

        try {
            chain.doFilter(request, response);
        } finally {
            log.info("← {} {} [status={}] [{}ms]",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), System.currentTimeMillis() - start);
            MDC.clear();
        }
    }

    private String resolveUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "anonymous";
        }
        try {
            String token = authHeader.substring(7);
            return jwtUtil.isValid(token) ? hashEmail(jwtUtil.extractEmail(token)) : "anonymous";
        } catch (Exception e) {
            return "anonymous";
        }
    }

    // SHA-256 of email truncated to 8 hex chars — stable per user, not reversible
    private String hashEmail(String email) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(email.getBytes(StandardCharsets.UTF_8));
            return String.format("%02x%02x%02x%02x", hash[0], hash[1], hash[2], hash[3]);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
