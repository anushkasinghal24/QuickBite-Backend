package com.quickbite.delivery_service.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * FeignConfig
 *
 * Automatically forwards Authorization (JWT) header to all outgoing Feign calls.
 * Same pattern as cart-service/config/FeignConfig.java
 *
 * Used when delivery-service calls:
 *  - notification-service: POST /api/v1/notifications/send
 *  - order-service (future): GET /api/v1/orders/{orderId}
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor jwtRequestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    requestTemplate.header("Authorization", authHeader);
                }
            }
        };
    }
}
