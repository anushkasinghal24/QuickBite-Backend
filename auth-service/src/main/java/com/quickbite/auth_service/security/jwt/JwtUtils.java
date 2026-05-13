package com.quickbite.auth_service.security.jwt;

import com.quickbite.auth_service.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtUtils — JWT Token generation, parsing, and validation.
 *
 * Access Token payload (claims):
 *  - sub     : user email
 *  - userId  : user ID (used by all other services as foreign key)
 *  - role    : CUSTOMER | OWNER | AGENT | ADMIN
 *  - isActive: boolean
 *  - iat     : issued at
 *  - exp     : expiry (24 hours default)
 *
 * WHY userId in JWT?
 *  → Other microservices extract userId from token claims so they DON'T
 *    need to call auth-service for every request. Only validation needed.
 *
 * Secret Key: Base64-encoded 256-bit key stored in Config Server / application.yml
 */
@Component
@Slf4j
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private Long jwtExpirationMs;

    // ── Token Generation ──────────────────────────────────────────────────────

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",   user.getUserId());
        claims.put("role",     user.getRole().name());
        claims.put("isActive", user.getIsActive());
        claims.put("provider", user.getProvider().name());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Token Parsing ─────────────────────────────────────────────────────────

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Integer extractUserId(String token) {
        return extractAllClaims(token).get("userId", Integer.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public Long getExpirationMs() {
        return jwtExpirationMs;
    }

    // ── Token Validation ──────────────────────────────────────────────────────

    /**
     * Full validation: signature + expiry.
     * Called by:
     *  1. JwtAuthenticationFilter (in this service, for its own protected endpoints)
     *  2. POST /api/v1/auth/validate-token (for API Gateway and other services)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.error("JWT signature validation failed: {}", e.getMessage());
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
