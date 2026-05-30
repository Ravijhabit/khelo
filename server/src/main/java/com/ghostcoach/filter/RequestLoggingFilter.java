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
import java.util.UUID;

/**
 * Runs first on every request to establish telemetry context in MDC.
 *
 * Sets two MDC keys that appear in every subsequent log line for this thread:
 *   - requestId: short UUID identifying this specific HTTP request
 *   - userId:    masked email (e.g. ra***@gm***.com) — identifiable but not fully exposed
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
            return jwtUtil.isValid(token) ? maskEmail(jwtUtil.extractEmail(token)) : "anonymous";
        } catch (Exception e) {
            return "anonymous";
        }
    }

    // ra***@gm***.com — enough to spot the user in logs without exposing the full email
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at < 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        int dot = domain.lastIndexOf('.');
        String maskedLocal = (local.length() > 2 ? local.substring(0, 2) : local) + "***";
        String maskedDomain = (dot > 0 ? domain.substring(0, 1) + "***" + domain.substring(dot) : "***");
        return maskedLocal + "@" + maskedDomain;
    }
}
