package com.ghostcoach.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every incoming HTTP request exactly once to extract and validate
 * the JWT from the Authorization header. If valid, populates the Spring Security
 * context so downstream controllers can access the authenticated principal via
 * {@code Authentication.getName()}.
 *
 * <p>Requests without a valid token pass through unauthenticated — the
 * {@code SecurityFilterChain} decides which endpoints require authentication.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Core filter logic. Reads the {@code Authorization: Bearer <token>} header,
     * validates the JWT, and if genuine, loads the user from the database and
     * sets the authentication in the {@link SecurityContextHolder}.
     *
     * <p>We always call {@code chain.doFilter()} regardless of auth outcome so
     * that public endpoints (e.g. /api/auth/**) are never blocked here — they
     * are permitted by the security chain configuration, not by this filter.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.isValid(token)) {
                String email = jwtUtil.extractEmail(token);
                // Re-load user from DB to pick up any role/status changes since token was issued
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
