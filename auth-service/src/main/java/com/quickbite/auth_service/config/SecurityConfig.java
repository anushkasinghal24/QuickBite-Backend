package com.quickbite.auth_service.config;

import com.quickbite.auth_service.security.jwt.JwtAuthenticationFilter;
import com.quickbite.auth_service.security.oauth2.CustomOAuth2AuthorizationRequestResolver;
import com.quickbite.auth_service.security.oauth2.CustomOAuth2UserService;
import com.quickbite.auth_service.security.oauth2.handler.OAuth2FailureHandler;
import com.quickbite.auth_service.security.oauth2.handler.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.quickbite.auth_service.service.impl.UserDetailsServiceImpl;

/**
 * SecurityConfig
 *
 * PUBLIC endpoints (no JWT required):
 *   POST /api/v1/auth/register
 *   POST /api/v1/auth/login
 *   POST /api/v1/auth/refresh
 *   POST /api/v1/auth/validate-token   ← API Gateway + other services call this
 *   GET  /api/v1/auth/users/{id}       ← Internal service-to-service call
 *   /oauth2/**                         ← OAuth2 flow
 *   /actuator/health                   ← Eureka health check
 *   /swagger-ui/**                     ← API docs
 *
 * PROTECTED endpoints (JWT required):
 *   GET  /api/v1/auth/profile          ← logged-in user
 *   PUT  /api/v1/auth/profile          ← update profile
 *   PUT  /api/v1/auth/password         ← change password
 *   POST /api/v1/auth/logout           ← invalidate refresh token
 *   DELETE /api/v1/auth/deactivate     ← self-deactivate
 *   /api/v1/auth/admin/**              ← ADMIN only
 *
 * FUTURE CHANGES when other services added:
 *   → No changes needed here.
 *   → Other services have their OWN SecurityConfig.
 *   → They call POST /api/v1/auth/validate-token to verify JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl  userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler    oAuth2SuccessHandler;
    private final OAuth2FailureHandler    oAuth2FailureHandler;
    private final CustomOAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // ── Public endpoints ──────────────────────────────────────────
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/validate-token"   // Critical: called by Gateway
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Internal service-to-service: fetch user by ID (no JWT, internal network only)
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/auth/users/{id}",
                    "/api/v1/auth/users/email/{email}"
                ).permitAll()

                // OAuth2 endpoints
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                // Actuator: Eureka health check
                .requestMatchers("/actuator/**").permitAll()

                // Swagger / OpenAPI docs
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**"
                ).permitAll()

                // ── Admin only ────────────────────────────────────────────────
                .requestMatchers("/api/v1/auth/admin/**").hasRole("ADMIN")

                // ── Everything else needs JWT ─────────────────────────────────
                .anyRequest().authenticated()
            )
            // ── OAuth2 Login ──────────────────────────────────────────────────
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth ->
                    auth.authorizationRequestResolver(oauth2AuthorizationRequestResolver))
                .userInfoEndpoint(userInfo ->
                    userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            // ── JWT Filter ────────────────────────────────────────────────────
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // ── Auth Provider ─────────────────────────────────────────────────
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // strength 12 for production
    }
}
