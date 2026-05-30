package com.ghostcoach.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String EMAIL = "player@example.com";
    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!";
    private static final long EXPIRATION_MS = 3_600_000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsThreePartJwt() {
        String token = jwtUtil.generateToken(EMAIL);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractEmail_roundTrip_returnsOriginalEmail() {
        String token = jwtUtil.generateToken(EMAIL);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo(EMAIL);
    }

    @Test
    void isValid_freshToken_returnsTrue() {
        String token = jwtUtil.generateToken(EMAIL);
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    void isValid_tamperedSignature_returnsFalse() {
        String token = jwtUtil.generateToken(EMAIL) + "corrupted";
        assertThat(jwtUtil.isValid(token)).isFalse();
    }

    @Test
    void isValid_blankString_returnsFalse() {
        assertThat(jwtUtil.isValid("")).isFalse();
    }

    @Test
    void isValid_randomString_returnsFalse() {
        assertThat(jwtUtil.isValid("not.a.real.jwt")).isFalse();
    }

    @Test
    void isValid_expiredToken_returnsFalse() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 1L);
        String token = jwtUtil.generateToken(EMAIL);
        Thread.sleep(50);
        assertThat(jwtUtil.isValid(token)).isFalse();
    }

    @Test
    void generateToken_withShortSecret_padsAndProducesValidToken() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "short");
        String token = jwtUtil.generateToken(EMAIL);
        assertThat(jwtUtil.isValid(token)).isTrue();
    }
}
