package com.photovault.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JwtUtil — specifically validates the JJWT 0.12.x API migration.
 * Prior code used parserBuilder()/parseClaimsJws()/getBody() which were removed in 0.12.x.
 * Fixed code uses parser()/parseSignedClaims()/getPayload().
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Must be at least 256 bits (32 chars) for HMAC-SHA256
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-algorithm";
    private static final long EXPIRATION = 86400000L; // 24h

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    @Test
    void generateToken_fromUserDetails_returnsNonNullToken() {
        UserDetails user = User.withUsername("test@example.com")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

        String token = jwtUtil.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateToken_withExtraClaims_embedsClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("photographerId", "abc-123");
        claims.put("plan", "pro");

        String token = jwtUtil.generateToken("test@example.com", claims);

        assertNotNull(token);
        // Verify we can extract the subject back out
        String username = jwtUtil.extractUsername(token);
        assertEquals("test@example.com", username);
    }

    @Test
    void extractUsername_returnsCorrectEmail() {
        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtil.generateToken("photographer@studio.com", claims);

        String username = jwtUtil.extractUsername(token);

        assertEquals("photographer@studio.com", username);
    }

    @Test
    void validateToken_withUserDetails_returnsTrueForValidToken() {
        UserDetails user = User.withUsername("test@example.com")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

        String token = jwtUtil.generateToken(user);

        assertTrue(jwtUtil.validateToken(token, user));
    }

    @Test
    void validateToken_withWrongUser_returnsFalse() {
        UserDetails user1 = User.withUsername("user1@example.com")
            .password("password")
            .authorities(Collections.emptyList())
            .build();
        UserDetails user2 = User.withUsername("user2@example.com")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

        String token = jwtUtil.generateToken(user1);

        assertFalse(jwtUtil.validateToken(token, user2));
    }

    @Test
    void validateToken_withTokenOnly_returnsTrueForValidToken() {
        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtil.generateToken("test@example.com", claims);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_withTamperedToken_returnsFalse() {
        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtil.generateToken("test@example.com", claims);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    void validateToken_withTokenSignedByDifferentKey_returnsFalse() {
        JwtUtil otherUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherUtil, "secret", "completely-different-secret-key-with-enough-bits");
        ReflectionTestUtils.setField(otherUtil, "expiration", EXPIRATION);

        Map<String, Object> claims = new HashMap<>();
        String foreignToken = otherUtil.generateToken("test@example.com", claims);

        assertFalse(jwtUtil.validateToken(foreignToken));
    }

    @Test
    void extractExpiration_returnsDateInFuture() {
        Map<String, Object> claims = new HashMap<>();
        String token = jwtUtil.generateToken("test@example.com", claims);

        var expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.getTime() > System.currentTimeMillis());
    }

    @Test
    void tokenRoundTrip_preservesSubject() {
        String email = "studio@photovault.com";
        Map<String, Object> claims = Map.of("plan", "studio", "photographerId", "uuid-123");

        String token = jwtUtil.generateToken(email, claims);
        String extracted = jwtUtil.extractUsername(token);

        assertEquals(email, extracted);
    }
}
