package com.quickbite.delivery_service.config;

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
 * Public endpoints (no JWT):
 *   GET /api/v1/agents/nearby    — order-service internal call (no user token)
 *   /actuator/health             — health checks
 *   /swagger-ui/**, /api-docs/** — Swagger UI
 *
 * Protected endpoints:
 *   POST /api/v1/agents/register       → AGENT only
 *   GET  /api/v1/agents/my             → AGENT only
 *   PUT  /{id}/location                → AGENT
 *   PUT  /{id}/availability            → AGENT
 *   POST /{id}/complete/{orderId}      → AGENT
 *   PUT  /{id}/verify                  → ADMIN
 *   GET  /all, /active, /status/**     → ADMIN
 *   DELETE /{id}                       → ADMIN
 *   PUT  /{id}/rating                  → Internal (X-Internal-Service header)
 *   POST /{id}/assign-order            → Internal (order-service)
 *   GET  /{id}/location                → Any authenticated (CUSTOMER tracking)
 *   GET  /{id}/earnings                → AGENT/ADMIN
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

                // ── PUBLIC ───────────────────────────────────────────────────
                // Actuator, Swagger
                .requestMatchers(
                    "/actuator/health", "/actuator/info",
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/api-docs/**", "/api-docs"
                ).permitAll()

                // nearby agents — called by order-service (internal) without user context
                // In production restrict this to internal VPC only
                .requestMatchers(HttpMethod.GET, "/api/v1/agents/nearby").permitAll()

                // ── AGENT role ────────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/agents/register").hasRole("AGENT")
                .requestMatchers(HttpMethod.GET,  "/api/v1/agents/my").hasRole("AGENT")
                .requestMatchers(HttpMethod.PUT,  "/api/v1/agents/*/availability").hasRole("AGENT")
                .requestMatchers(HttpMethod.GET,  "/api/v1/agents/*/assigned-order").hasAnyRole("AGENT", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/agents/*/pickup/*").hasRole("AGENT")
                .requestMatchers(HttpMethod.POST, "/api/v1/agents/*/complete/*").hasRole("AGENT")

                // ── ADMIN role ────────────────────────────────────────────────
                .requestMatchers(HttpMethod.PUT,    "/api/v1/agents/*/verify").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/v1/agents/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/v1/agents/active").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/v1/agents/status/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/agents/**").hasRole("ADMIN")

                // ── All other endpoints — must be authenticated (any role) ────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
