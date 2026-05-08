package com.quickbite.order.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/orders").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/orders/*/reorder").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/orders/*/cancel").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/orders/customer/*").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/orders/customer/*/active").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/orders/restaurant/*").hasAnyRole("OWNER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/orders/restaurant/*/analytics").hasAnyRole("OWNER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/orders/*/accept").hasAnyRole("OWNER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/orders/agent/*").hasAnyRole("AGENT", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/orders/*/status").hasAnyRole("OWNER", "AGENT", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/orders/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/orders/all/active").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/orders/*/agent").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/orders/count/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/cart/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/cart/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);
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
}
