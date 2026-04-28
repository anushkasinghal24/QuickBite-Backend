package com.quickbite.delivery_service.config;

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
 * EXACT same pattern as restaurant-service/config/JwtUtils.java
 *
 * JWT secret MUST match auth-service exactly.
 * delivery-service never generates tokens — only validates.
 *
 * Claims extracted:
 *  - sub    → email (String)
 *  - userId → Integer (used as principal in SecurityContext)
 *  - role   → CUSTOMER | OWNER | AGENT | ADMIN
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
            log.error("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims empty: {}", e.getMessage());
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

    /**
     * Returns userId (Integer) from JWT claims.
     * Used as principal in SecurityContext — same as restaurant-service pattern.
     */
    public Integer getUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        return userId != null ? Integer.valueOf(userId.toString()) : null;
    }

    public String getRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public String getEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractAllClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}
