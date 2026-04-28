package com.quickbite.order.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
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
}
