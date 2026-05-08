package com.quickbite.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * QuickBite Auth Service - Entry Point
 *
 * Responsibilities:
 *  - User Registration & Login (email/password)
 *  - Google / GitHub OAuth2 Login
 *  - JWT Token generation, validation, refresh
 *  - Role management: CUSTOMER, OWNER, AGENT, ADMIN
 *  - Profile management & password change
 *  - Account deactivation
 *  - Token validation endpoint for other microservices (Gateway / Feign)
 */
@SpringBootApplication
@EnableDiscoveryClient   // Registers with Eureka
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
