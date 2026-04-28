package com.quickbite.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * QuickBite - Restaurant Service
 *
 * Responsibilities:
 *  - Restaurant profile registration & management
 *  - Admin approval workflow
 *  - Geo-proximity based discovery (Haversine)
 *  - Open/Close toggle
 *  - avgRating update (called by review-service via Feign)
 *  - Menu category & item management (lives here alongside restaurant)
 *
 * Port: 8082
 * DB  : quickbite_restaurant (MySQL)
 * Cache: Redis (restaurant listings)
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {
                "com.quickbite.restaurant",
                "com.quickbite.review_service"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.quickbite\\.review_service\\.config\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.quickbite\\.review_service\\..*Application")
        }
)
@EnableDiscoveryClient          // Eureka registration
@EntityScan(basePackages = "com.quickbite")
@EnableJpaRepositories(basePackages = "com.quickbite")
public class RestaurantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantServiceApplication.class, args);
    }
}
