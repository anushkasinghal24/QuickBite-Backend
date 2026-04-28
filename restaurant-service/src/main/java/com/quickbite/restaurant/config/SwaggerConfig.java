package com.quickbite.restaurant.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SwaggerConfig — OpenAPI 3.0 documentation.
 * Access at: http://localhost:8082/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI restaurantServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QuickBite — Restaurant Service API")
                        .description("""
                                Restaurant profile management, menu management, and geo-proximity discovery.
                                
                                **Public endpoints** (no JWT): GET /restaurants, /search, /nearby, /menu
                                
                                **Protected endpoints**: Require Bearer JWT from auth-service.
                                - OWNER: Register, Update, Toggle Open, Manage Menu
                                - ADMIN: Approve/Reject, Delete
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QuickBite Engineering")
                                .email("dev@quickbite.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste JWT token from auth-service /auth/login response")));
    }
}
