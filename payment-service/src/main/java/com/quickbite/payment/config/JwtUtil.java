package com.quickbite.payment.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JwtUtil â€” validates tokens issued by auth-service.
 * Pattern matches cart-service JwtUtil exactly.
 *
 * MUST use same secret as auth-service:
 *   jwt.secret=QuickBiteSecretKeyForJWTTokenGenerationAndValidation2026
 *
 * Extracted claims:
 *   sub   â†’ email
 *   userId â†’ int (matches cart-service pattern)
 *   role  â†’ CUSTOMER / OWNER / AGENT / ADMIN
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
        }
        return false;
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    /** Returns userId as Long â€” payment-service uses Long IDs */
    public Long extractUserId(String token) {
        Object id = getClaims(token).get("userId");
        return id != null ? Long.parseLong(id.toString()) : null;
    }

    public boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
