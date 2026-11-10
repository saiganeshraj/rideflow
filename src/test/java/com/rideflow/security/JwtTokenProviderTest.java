package com.rideflow.security;

import com.rideflow.user.Role;
import com.rideflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final String VALID_SECRET = "a-very-secure-and-sufficiently-long-secret-key-of-at-least-256-bits!";
    private static final long EXPIRATION_MS = 900000; // 15 minutes

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(VALID_SECRET, EXPIRATION_MS);
    }

    @Test
    void testGenerateAndValidateToken() {
        User user = new User("John Test", "john@example.com", "+1234567890", "$2a$10$hash", Role.CUSTOMER);
        user.setId(UUID.randomUUID());

        String token = jwtTokenProvider.generateToken(user);
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        assertEquals(user.getId(), jwtTokenProvider.getUserId(token));
        assertEquals("john@example.com", jwtTokenProvider.getEmail(token));
        assertEquals(Role.CUSTOMER, jwtTokenProvider.getRole(token));
        assertEquals(900, jwtTokenProvider.getExpiresInSeconds());
    }

    @Test
    void testTamperedTokenReturnsFalse() {
        User user = new User("John Test", "john@example.com", "+1234567890", "$2a$10$hash", Role.CUSTOMER);
        user.setId(UUID.randomUUID());

        String token = jwtTokenProvider.generateToken(user);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    void testExpiredTokenReturnsFalse() {
        // Provider configured with negative expiration (-10 seconds)
        JwtTokenProvider expiredProvider = new JwtTokenProvider(VALID_SECRET, -10000);
        User user = new User("Expired User", "expired@example.com", null, "$2a$10$hash", Role.CUSTOMER);
        user.setId(UUID.randomUUID());

        String token = expiredProvider.generateToken(user);
        assertFalse(jwtTokenProvider.validateToken(token), "Expired token must not be valid");
    }

    @Test
    void testNullOrBlankTokenReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken("   "));
    }

    @Test
    void testMissingSecretThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(null, EXPIRATION_MS));
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("", EXPIRATION_MS));
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("   ", EXPIRATION_MS));
    }

    @Test
    void testShortSecretThrowsIllegalStateException() {
        // Less than 32 bytes (256 bits)
        String shortSecret = "too-short-secret";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtTokenProvider(shortSecret, EXPIRATION_MS));
        assertTrue(ex.getMessage().contains("at least 256 bits"));
    }
}
