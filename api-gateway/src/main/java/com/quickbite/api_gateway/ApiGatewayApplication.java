package com.quickbite.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * QuickBite API Gateway — Entry Point
 *
 * PORT: 8080 (single entry point for entire platform)
 *
 * Responsibilities:
 *  1. Route requests to correct microservice (via Eureka load balancer)
 *  2. JWT Authentication — validate token, extract userId + role
 *  3. Inject user context headers (X-User-Id, X-User-Role, X-User-Email)
 *     so downstream services don't need to parse JWT themselves
 *  4. CORS configuration for frontend (React / Thymeleaf)
 *  5. Public route whitelisting (no JWT needed for browse/login/register)
 *
 * Services routed (grows as project grows):
 *  auth-service        → /api/v1/auth/**
 *  restaurant-service  → /api/v1/restaurants/**
 *  menu-service        → /api/v1/menu/**
 *  cart-service        → /api/v1/cart/**
 *  order-service       → /api/v1/orders/**
 *  payment-service     → /api/v1/payments/**, /api/v1/wallet/**
 *  delivery-service    → /api/v1/agents/**
 *  review-service      → /api/v1/reviews/**
 *  notification-service→ /api/v1/notifications/**
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
