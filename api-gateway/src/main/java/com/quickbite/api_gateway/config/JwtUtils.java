package com.quickbite.api_gateway.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JwtUtils (Gateway)
 *
 * The Gateway parses JWT LOCALLY — it does NOT call auth-service for every request.
 * This is the correct microservices pattern:
 *
 *   Gateway has the SAME jwt.secret as auth-service.
 *   It validates signature + expiry locally (fast, no network hop).
 *   Extracts: userId, role, email from claims.
 *   Injects them as headers: X-User-Id, X-User-Role, X-User-Email.
 *   Downstream services read these headers — they trust Gateway.
 *
 * auth-service's POST /validate-token is for:
 *   - Other services that need to do ad-hoc validation outside Gateway
 *   - NOT used by Gateway (would create circular dependency + latency)
 *
 * FUTURE: When you add more services, JWT secret must remain SAME.
 * Store it in Config Server so all services share it automatically.
 */
@Component
@Slf4j
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Gateway: JWT expired — {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Gateway: JWT malformed — {}", e.getMessage());
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.warn("Gateway: JWT signature invalid — {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Gateway: JWT error — {}", e.getMessage());
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

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Integer extractUserId(String token) {
        return extractAllClaims(token).get("userId", Integer.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiry = extractAllClaims(token).getExpiration();
            return expiry.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
