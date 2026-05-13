package com.quickbite.api_gateway.filter;

import com.quickbite.api_gateway.config.JwtUtils;
import com.quickbite.api_gateway.config.RouteValidator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JwtAuthenticationFilter — Global filter applied to EVERY request.
 *
 * Flow for each incoming request:
 *
 *  1. Check if route is PUBLIC (RouteValidator)
 *     → If public: skip JWT, forward request as-is
 *     → Special case: GET /api/v1/restaurants & GET /api/v1/menu are public
 *                     but POST/PUT/DELETE on same paths need JWT
 *
 *  2. For protected routes:
 *     a. Check Authorization header exists → 401 if missing
 *     b. Extract Bearer token
 *     c. Validate JWT locally (signature + expiry) → 401 if invalid
 *     d. Extract claims: userId, role, email
 *     e. Check isActive in claims → 403 if account suspended
 *     f. Inject headers into request before forwarding:
 *        X-User-Id    → downstream services use this as customerId/ownerId/agentId
 *        X-User-Role  → downstream services use for authorization decisions
 *        X-User-Email → notification-service uses this for emails
 *        X-User-Name  → for logging / display
 *
 *  3. Forward to correct microservice via Eureka load balancer (lb://)
 *
 * WHY inject headers?
 *   Without headers, every downstream service would need the JWT secret
 *   and would parse the token themselves — duplicated code.
 *   With headers, downstream services just read X-User-Id from request.
 *
 * HOW downstream services use these headers:
 *   @RequestHeader("X-User-Id") Integer userId
 *   @RequestHeader("X-User-Role") String role
 *
 * FUTURE CHANGES:
 *   → Adding new microservice? No changes needed here.
 *     Just add its route in application.yml.
 *   → Adding new role? Just add role-based checks in downstream service.
 *   → Adding rate limiting? Add Redis-based filter (RateLimitFilter) separately.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // Run this filter first, before anything else
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter {

    private final RouteValidator routeValidator;
    private final JwtUtils       jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path   = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        log.debug("Gateway: {} {}", method, path);

        // Let CORS preflight requests pass through without JWT checks.
        // These requests never carry Authorization headers.
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        // ── STEP 1: Public GET routes for restaurant/menu browsing ────────────
        // PDF: "Guests can browse restaurants, view menus, and search without logging in"
        boolean isPublicGetRoute = (
                path.startsWith("/api/v1/restaurants") || path.startsWith("/api/v1/menu")
                || path.startsWith("/api/v1/reviews/restaurant")
        ) && HttpMethod.GET.equals(method);

        // ── STEP 2: Check if route needs JWT ──────────────────────────────────
        boolean isSecuredRoute = routeValidator.isSecured.test(request);

        if (!isSecuredRoute || isPublicGetRoute) {
            // Public route — forward without JWT check
            log.debug("Gateway: Public route, forwarding → {}", path);
            return chain.filter(exchange);
        }

        // ── STEP 3: Extract Authorization header ──────────────────────────────
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            log.warn("Gateway: Missing Authorization header for {}", path);
            return sendUnauthorized(exchange, "Authorization header is missing");
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorized(exchange, "Authorization header must start with 'Bearer '");
        }

        String token = authHeader.substring(7);

        // ── STEP 4: Validate JWT locally ──────────────────────────────────────
        if (!jwtUtils.validateToken(token)) {
            log.warn("Gateway: Invalid/expired JWT for path {}", path);
            return sendUnauthorized(exchange, "Token is invalid or expired. Please login again.");
        }

        // ── STEP 5: Extract claims and inject headers ─────────────────────────
        try {
            Claims claims    = jwtUtils.extractAllClaims(token);
            String email     = claims.getSubject();
            Integer userId   = claims.get("userId",   Integer.class);
            String role      = claims.get("role",     String.class);
            Boolean isActive = claims.get("isActive", Boolean.class);

            // Check if account is suspended
            if (Boolean.FALSE.equals(isActive)) {
                log.warn("Gateway: Suspended account attempted access — userId: {}", userId);
                return sendForbidden(exchange, "Your account has been suspended. Contact support.");
            }

            log.debug("Gateway: Authenticated userId={} role={} → {}", userId, role, path);

            // ── STEP 6: Mutate request — add user context headers ─────────────
            // Downstream services read these headers instead of parsing JWT
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id",    String.valueOf(userId))
                    .header("X-User-Role",  role)
                    .header("X-User-Email", email)
                    .header("X-Forwarded-By", "api-gateway")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.error("Gateway: Error processing JWT claims — {}", e.getMessage());
            return sendUnauthorized(exchange, "Token processing failed. Please login again.");
        }
    }

    // ── Helper: 401 Unauthorized ──────────────────────────────────────────────

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format(
                "{\"success\":false,\"message\":\"%s\",\"status\":401}", message);
        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    // ── Helper: 403 Forbidden ─────────────────────────────────────────────────

    private Mono<Void> sendForbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format(
                "{\"success\":false,\"message\":\"%s\",\"status\":403}", message);
        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}
