package com.quickbite.review_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * AppConfig — bean definitions for review-service.
 *
 * Follows same pattern as restaurant-service/config/AppConfig.java
 */
@Configuration
public class AppConfig {

    /** ModelMapper — same as restaurant-service */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
              .setMatchingStrategy(MatchingStrategies.STRICT)
              .setSkipNullEnabled(true);
        return mapper;
    }

    /**
     * Feign Request Interceptor — forwards JWT to downstream services.
     * Same pattern as cart-service/config/FeignConfig.java
     *
     * review-service calls:
     *  - restaurant-service: PUT /api/v1/restaurants/{id}/rating
     *  - delivery-service:   PUT /api/v1/agents/{id}/rating
     *  - notification-service: POST /api/v1/notifications/send
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /** Swagger/OpenAPI — same pattern as restaurant-service */
    @Bean
    public OpenAPI reviewServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QuickBite — Review & Rating Service")
                        .description("Dual rating system: food quality (restaurant) + delivery experience (agent). " +
                                     "One review per order. Average ratings pushed to restaurant & delivery services.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
