package com.quickbite.api_gateway.filter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * FallbackController
 *
 * Called by Circuit Breaker when a downstream service is unavailable.
 * Instead of showing a 500 error, returns a friendly JSON message.
 *
 * Circuit Breaker opens when:
 *  - Service throws 5xx errors > 50% of the time (last 10 requests)
 *  - Service doesn't respond within timeout
 *
 * When circuit is OPEN:
 *  - Requests immediately fail with fallback response (no network call)
 *  - After 10 seconds, circuit goes HALF-OPEN (tries 1 request)
 *  - If that succeeds → CLOSED (back to normal)
 *
 * FUTURE CHANGES:
 *  As new services are added, add their fallback endpoints here.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private Mono<ResponseEntity<Map<String, Object>>> fallback(String service) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "message", service + " is currently unavailable. Please try again in a moment.",
                        "status", 503
                )));
    }

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback() {
        return fallback("Auth Service");
    }

    @GetMapping("/restaurant")
    public Mono<ResponseEntity<Map<String, Object>>> restaurantFallback() {
        return fallback("Restaurant Service");
    }

    @GetMapping("/menu")
    public Mono<ResponseEntity<Map<String, Object>>> menuFallback() {
        return fallback("Menu Service");
    }

    @GetMapping("/cart")
    public Mono<ResponseEntity<Map<String, Object>>> cartFallback() {
        return fallback("Cart Service");
    }

    @GetMapping("/order")
    public Mono<ResponseEntity<Map<String, Object>>> orderFallback() {
        return fallback("Order Service");
    }

    @GetMapping("/payment")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback() {
        return fallback("Payment Service");
    }

    @GetMapping("/delivery")
    public Mono<ResponseEntity<Map<String, Object>>> deliveryFallback() {
        return fallback("Delivery Service");
    }

    @GetMapping("/review")
    public Mono<ResponseEntity<Map<String, Object>>> reviewFallback() {
        return fallback("Review Service");
    }

    @GetMapping("/notification")
    public Mono<ResponseEntity<Map<String, Object>>> notificationFallback() {
        return fallback("Notification Service");
    }
}
