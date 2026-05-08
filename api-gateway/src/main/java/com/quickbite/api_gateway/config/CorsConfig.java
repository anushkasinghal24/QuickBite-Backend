package com.quickbite.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.util.List;

/**
 * CorsConfig
 *
 * Centralized CORS config at Gateway level.
 * Without this, browser requests from React/Thymeleaf frontend get blocked.
 *
 * FUTURE CHANGES:
 *   → When deploying to production:
 *     Replace "http://localhost:3000" with actual frontend domain.
 *     E.g., "https://quickbite.com", "https://app.quickbite.com"
 *   → If you add a mobile app: mobile apps don't need CORS.
 *   → If you add multiple frontend origins, add them to allowedOrigins list.
 */
@Configuration
@EnableWebFlux
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Frontend origins allowed to call the API
        // Allow the common local dev origins plus any localhost/127.0.0.1 port.
        // This keeps registration and login working even if the frontend runs on
        // a different dev port than the one listed in the static config.
        corsConfig.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        // All standard HTTP methods
        corsConfig.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allow all headers (Authorization, Content-Type, X-User-Id, etc.)
        corsConfig.setAllowedHeaders(List.of("*"));

        // Allow cookies / credentials (needed for session-based flows)
        corsConfig.setAllowCredentials(true);

        // Cache preflight for 1 hour
        corsConfig.setMaxAge(3600L);

        // Expose custom headers to frontend JavaScript
        corsConfig.setExposedHeaders(List.of(
                "Authorization",
                "X-User-Id",
                "X-User-Role",
                "X-User-Email"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @Bean
    public CorsWebFilter corsWebFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsWebFilter(corsConfigurationSource);
    }
}
