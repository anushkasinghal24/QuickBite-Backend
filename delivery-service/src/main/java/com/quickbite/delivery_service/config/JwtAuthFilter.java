package com.quickbite.delivery_service.config;

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
 * JwtAuthFilter
 *
 * EXACT same pattern as restaurant-service/config/JwtAuthFilter.java
 *
 * Flow:
 * 1. Extract Bearer token from Authorization header
 * 2. Validate with JwtUtils (same secret as auth-service)
 * 3. Extract userId (Integer) and role
 * 4. Set Authentication in SecurityContext:
 *    - principal = userId (Integer)   ← controller uses (Integer) authentication.getPrincipal()
 *    - credentials = email
 *    - authorities = ROLE_AGENT / ROLE_ADMIN etc.
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
                Integer userId = jwtUtils.getUserId(token);
                String  role   = jwtUtils.getRole(token);
                String  email  = jwtUtils.getEmail(token);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                // principal = userId (Integer) — consistent with restaurant-service pattern
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
