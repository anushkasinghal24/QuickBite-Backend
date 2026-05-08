package com.quickbite.review_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig
 *
 * EXACT same pattern as restaurant-service/config/SecurityConfig.java
 *
 * Public endpoints (no JWT — PDF: guests can read reviews):
 *   GET /api/v1/reviews/restaurant/**   — public review display
 *   GET /actuator/health
 *   /swagger-ui/**, /api-docs/**
 *
 * Protected endpoints:
 *   POST  /api/v1/reviews          → CUSTOMER (submit review)
 *   PUT   /api/v1/reviews/{id}     → CUSTOMER (update own)
 *   DELETE /api/v1/reviews/{id}    → CUSTOMER/ADMIN
 *   PUT   /api/v1/reviews/{id}/flag    → OWNER/ADMIN
 *   PUT   /api/v1/reviews/{id}/verify  → ADMIN
 *   GET   /api/v1/reviews/all          → ADMIN
 *   GET   /api/v1/reviews/flagged      → ADMIN
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ── PUBLIC ────────────────────────────────────────────────────
                // Guests can read restaurant reviews (PDF: "browse without logging in")
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/reviews/restaurant/**"
                ).permitAll()

                // Swagger + Actuator
                .requestMatchers(
                    "/actuator/health", "/actuator/info",
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/api-docs/**", "/api-docs"
                ).permitAll()

                // ── CUSTOMER ──────────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/reviews").hasRole("CUSTOMER")

                // ── ADMIN ─────────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/flagged").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/*/verify").hasRole("ADMIN")

                // ── OWNER or ADMIN can flag ────────────────────────────────────
                .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/*/flag")
                    .hasAnyRole("OWNER", "ADMIN")

                // ── All other endpoints — must be authenticated ────────────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
