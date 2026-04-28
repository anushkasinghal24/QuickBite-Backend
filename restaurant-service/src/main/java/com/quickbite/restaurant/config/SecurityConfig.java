package com.quickbite.restaurant.config;

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
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/restaurants",
                        "/api/v1/restaurants/{id}",
                        "/api/v1/restaurants/search",
                        "/api/v1/restaurants/nearby",
                        "/api/v1/restaurants/city/{city}",
                        "/api/v1/restaurants/cuisine/{cuisine}",
                        "/api/v1/restaurants/{id}/menu",
                        "/api/v1/restaurants/{id}/categories",
                        "/api/v1/restaurants/{id}/categories/{categoryId}/items",
                        "/api/v1/restaurants/{id}/items/search",
                        "/api/v1/restaurants/{id}/items/veg"
                ).permitAll()
                .requestMatchers(
                        "/swagger-ui/**", "/swagger-ui.html",
                        "/api-docs/**", "/api-docs",
                        "/actuator/health", "/actuator/info"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/restaurants").hasRole("OWNER")
                .requestMatchers(HttpMethod.PUT,
                        "/api/v1/restaurants/{id}",
                        "/api/v1/restaurants/{id}/toggle-open"
                ).hasRole("OWNER")
                .requestMatchers(HttpMethod.POST,
                        "/api/v1/restaurants/{id}/categories",
                        "/api/v1/restaurants/{id}/categories/{categoryId}/items"
                ).hasRole("OWNER")
                .requestMatchers(HttpMethod.PUT,
                        "/api/v1/restaurants/{id}/categories/{categoryId}",
                        "/api/v1/restaurants/{id}/items/{itemId}",
                        "/api/v1/restaurants/{id}/items/{itemId}/toggle-availability"
                ).hasRole("OWNER")
                .requestMatchers(HttpMethod.DELETE,
                        "/api/v1/restaurants/{id}/categories/{categoryId}",
                        "/api/v1/restaurants/{id}/items/{itemId}"
                ).hasRole("OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/restaurants/{id}/approve").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/restaurants/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/restaurants/admin/**",
                        "/api/v1/restaurants/pending"
                ).hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/restaurants/{id}/rating")
                        .hasAnyRole("ADMIN", "OWNER")
                .requestMatchers(HttpMethod.GET, "/api/v1/menu/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/menu/**").hasAnyRole("OWNER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/menu/**").hasAnyRole("OWNER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/menu/**").hasAnyRole("OWNER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/menu/**").hasAnyRole("OWNER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/restaurant/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/reviews").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/*").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/*").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/*/flag").hasAnyRole("OWNER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/*/verify").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/flagged").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/customer/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/order/**").hasAnyRole("CUSTOMER", "OWNER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/agent/**").hasAnyRole("AGENT", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
