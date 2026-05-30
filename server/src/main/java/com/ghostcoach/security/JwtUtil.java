package com.ghostcoach.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

/**
 * Handles all JWT operations: token creation, claim extraction, and validation.
 * Uses HMAC-SHA256 (HS256) signing. The secret is read from application properties
 * so it never appears in source code.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    /**
     * Derives a 256-bit HMAC key from the configured secret string.
     * Pads to 32 bytes if the secret is shorter than HS256's minimum requirement,
     * rather than rejecting short secrets at startup — keeps development friction low.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT with the user's email as the subject.
     * Email is used as the principal throughout Spring Security, so it doubles as
     * the username. Expiry is controlled by jwt.expiration (default 24 h).
     *
     * @param email the authenticated user's email address
     * @return compact, URL-safe JWT string
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the email (subject claim) from a verified token.
     * Only call this after confirming {@link #isValid(String)} returns true;
     * otherwise the parser will throw on tampered or expired tokens.
     *
     * @param token a signed JWT string
     * @return the email encoded in the subject claim
     */
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validates a JWT: checks signature integrity and expiry in one step.
     * Catches all jjwt exceptions so the filter can safely call this without
     * try-catch boilerplate — any invalid token simply returns false.
     *
     * @param token the JWT string from the Authorization header
     * @return true if the token is genuine and not expired
     */
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
