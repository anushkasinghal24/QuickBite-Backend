package com.quickbite.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * GlobalErrorHandler
 *
 * Catches unhandled exceptions at the Gateway level and returns
 * clean JSON error responses instead of Spring's default HTML error page.
 *
 * Handles:
 *  - Service not found in Eureka (503 Service Unavailable)
 *  - Connection refused (503)
 *  - Timeout (504 Gateway Timeout)
 *  - General server errors (500)
 */
@Configuration
@Order(-1)  // Higher priority than default Spring error handler
@Slf4j
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("Gateway Error: {} — {}", ex.getClass().getSimpleName(), ex.getMessage());

        HttpStatus status;
        String message;

        if (ex instanceof NotFoundException) {
            // Eureka: service not registered / no instances available
            status  = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service is currently unavailable. Please try again later.";
        } else if (ex instanceof ResponseStatusException rse) {
            status  = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : ex.getMessage();
        } else if (ex.getMessage() != null && ex.getMessage().contains("Connection refused")) {
            status  = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Unable to connect to the service. Please try again.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("timeout")) {
            status  = HttpStatus.GATEWAY_TIMEOUT;
            message = "Request timed out. Please try again.";
        } else {
            status  = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred at the gateway.";
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"success\":false,\"message\":\"%s\",\"status\":%d}",
                message, status.value()
        );

        var buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
