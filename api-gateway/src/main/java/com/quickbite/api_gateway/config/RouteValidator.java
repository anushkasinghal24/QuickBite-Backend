package com.quickbite.api_gateway.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * RouteValidator
 *
 * Central definition of PUBLIC routes (no JWT required).
 *
 * PUBLIC ROUTES — anyone can access (Guest + unauthenticated):
 *
 *  Auth:
 *   POST /api/v1/auth/register          ← new user signup
 *   POST /api/v1/auth/login             ← email/password login
 *   POST /api/v1/auth/refresh           ← get new access token
 *   /oauth2/**                          ← Google/GitHub OAuth2 flow
 *   /login/oauth2/**                    ← OAuth2 callback
 *
 *  Restaurant & Menu (Guest Browse — PDF requirement):
 *   GET  /api/v1/restaurants/**         ← browse restaurants (guests CAN browse)
 *   GET  /api/v1/menu/**               ← view menus (guests CAN view)
 *
 *  Internal (service-to-service):
 *   GET  /api/v1/auth/users/**          ← other services fetch user details
 *
 *  Infrastructure:
 *   /actuator/**                        ← health checks
 *   /eureka/**                          ← Eureka dashboard
 *
 * PROTECTED ROUTES — everything else needs JWT:
 *   POST /api/v1/cart/**               ← add to cart (CUSTOMER)
 *   POST /api/v1/orders/**             ← place order (CUSTOMER)
 *   /api/v1/payments/**               ← payment (CUSTOMER)
 *   /api/v1/wallet/**                 ← wallet (CUSTOMER)
 *   /api/v1/notifications/**          ← notifications (all roles)
 *   /api/v1/reviews/**                ← post review (CUSTOMER)
 *   /api/v1/agents/**                 ← delivery agent ops (AGENT)
 *   PUT/DELETE /api/v1/restaurants/** ← manage restaurant (OWNER)
 *   PUT/DELETE /api/v1/menu/**        ← manage menu (OWNER)
 *   /api/v1/auth/profile              ← profile management
 *   /api/v1/auth/admin/**             ← admin panel (ADMIN)
 *
 * FUTURE CHANGES:
 *   When you add a new service, add its public GET routes here if guests can see them.
 *   Protected routes need no change — they're automatically blocked by default.
 */
@Component
public class RouteValidator {

    /**
     * List of URI patterns that are OPEN (no JWT needed).
     * Uses simple startsWith / contains matching.
     */
    public static final List<String> OPEN_ENDPOINTS = List.of(
            // Auth — public
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",

            // OAuth2 flow — public
            "/oauth2/",
            "/login/oauth2/",

            // Internal service-to-service — no JWT (should be VPC-restricted in prod)
            "/api/v1/auth/users/",

            // Guest browsing — PDF: "Guests can browse restaurants, view menus"
            // Only GET methods — POST/PUT/DELETE still blocked
            // Method-level check happens in JwtAuthenticationFilter
            "/api/v1/restaurants",
            "/api/v1/menu",
            "/api/v1/reviews/restaurant/",

            // Infrastructure
            "/actuator",
            "/eureka"
    );

    /**
     * Returns true if the request URI is a SECURED endpoint (requires JWT).
     */
    public Predicate<ServerHttpRequest> isSecured =
            request -> OPEN_ENDPOINTS
                    .stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
