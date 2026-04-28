package com.quickbite.restaurant.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JwtAuthFilter — runs once per request, before Spring Security.
 *
 * Flow:
 * 1. Extract Bearer token from Authorization header
 * 2. Validate token using JwtUtils (same secret as auth-service)
 * 3. Parse userId and role from claims
 * 4. Set Authentication in SecurityContext
 *
 * api-gateway bhi JWT validate karta hai, but restaurant-service
 * independently validate karta hai (defense in depth).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = parseJwt(request);

            if (token != null && jwtUtils.validateToken(token)) {
                Long userId = jwtUtils.getUserId(token);
                String role  = jwtUtils.getRole(token);
                String email = jwtUtils.getEmail(token);

                // Create authentication with role as authority
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, email, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT valid — userId={}, role={}", userId, role);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
