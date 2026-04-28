package com.quickbite.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * FeignConfig
 *
 * Propagates the incoming JWT Bearer token to all downstream
 * Feign client calls (cart-service, restaurant-service, etc.).
 *
 * Without this, Feign calls to JWT-protected services would fail
 * with 401 Unauthorized.
 *
 * Same pattern as cart-service FeignConfig.
 */
@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                try {
                    ServletRequestAttributes attributes =
                            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                    if (attributes != null) {
                        String authHeader = attributes.getRequest()
                                                      .getHeader("Authorization");
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            template.header("Authorization", authHeader);
                            log.debug("Forwarded JWT to downstream service: {}",
                                    template.url());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not propagate JWT to Feign request: {}", e.getMessage());
                }
            }
        };
    }
}
