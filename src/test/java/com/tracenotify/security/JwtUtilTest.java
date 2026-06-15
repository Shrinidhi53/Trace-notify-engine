package com.tracenotify.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(
            "test-secret-that-is-definitely-long-enough-for-hmac-sha-256-key", 3600000L);

    @Test
    void generatesAndParsesToken() {
        String token = jwtUtil.generateToken("user-123", "alice", "ADMIN");
        assertTrue(jwtUtil.isValid(token));
        assertEquals("user-123", jwtUtil.extractUserId(token));
        assertEquals("alice", jwtUtil.extractUsername(token));
        assertEquals("ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void rejectsGarbageToken() {
        assertFalse(jwtUtil.isValid("not-a-real-token"));
    }

    @Test
    void rejectsExpiredToken() {
        JwtUtil shortLived = new JwtUtil(
                "test-secret-that-is-definitely-long-enough-for-hmac-sha-256-key", -1000L);
        String token = shortLived.generateToken("u", "n", "USER");
        assertFalse(shortLived.isValid(token));
    }
}
