package com.quickbite.payment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QuickBite â€” Payment & Wallet Service API")
                        .description("""
                                Handles order payment processing, wallet management, refunds, and transaction history.
                                
                                **Payment Modes**: COD, CARD, UPI, WALLET
                                
                                **Wallet**: Top-up via CARD/UPI, auto-debit on wallet payment, refund credited to wallet.
                                
                                **Internal endpoints** (called by order-service):
                                - POST /api/v1/payments/process
                                - POST /api/v1/payments/{id}/refund
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("QuickBite Engineering").email("dev@quickbite.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from auth-service /api/v1/auth/login")));
    }
}
