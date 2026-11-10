package com.rideflow.security;

import com.rideflow.user.Role;
import com.rideflow.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Service for issuing and validating JWT access tokens using JJWT 0.12.x.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:${JWT_SECRET:}}") String secret,
            @Value("${jwt.expiration-ms:${JWT_EXPIRATION_MS:900000}}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT signing secret is required. Please set the JWT_SECRET environment variable.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT signing secret must be at least 256 bits (32 bytes) long. Provided secret is too short.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT access token for the given user.
     *
     * @param user the user details
     * @return signed JWT string
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("fullName", user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates whether a token signature is valid and unexpired.
     *
     * @param token the JWT string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Parses claims from a signed JWT token.
     *
     * @param token the JWT string
     * @return parsed Claims payload
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts user ID from the token subject.
     */
    public UUID getUserId(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts email from token claims.
     */
    public String getEmail(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Extracts user role from token claims.
     */
    public Role getRole(String token) {
        Claims claims = parseClaims(token);
        String roleStr = claims.get("role", String.class);
        return Role.valueOf(roleStr);
    }

    /**
     * Returns token expiration duration in seconds.
     */
    public long getExpiresInSeconds() {
        return expirationMs / 1000;
    }

    /**
     * Returns token expiration duration in milliseconds.
     */
    public long getExpirationMs() {
        return expirationMs;
    }
}
