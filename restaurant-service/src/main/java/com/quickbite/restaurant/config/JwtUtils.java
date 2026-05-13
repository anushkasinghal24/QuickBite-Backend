package com.quickbite.restaurant.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JwtUtils — validates JWT tokens issued by auth-service.
 *
 * IMPORTANT: jwt.secret MUST be identical to auth-service's jwt.secret.
 * In production, both services load this from Spring Cloud Config Server.
 *
 * Extracted claims used:
 *  - sub  → userId (String)
 *  - role → CUSTOMER / OWNER / AGENT / ADMIN
 *  - email → user's email
 */
@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

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
        }
        return false;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        Number userId = extractAllClaims(token).get("userId", Number.class);
        if (userId != null) {
            return userId.longValue();
        }

        String legacyUserId = extractAllClaims(token).getSubject();
        try {
            return Long.parseLong(legacyUserId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("JWT does not contain a valid userId claim");
        }
    }

    public String getRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public String getEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
