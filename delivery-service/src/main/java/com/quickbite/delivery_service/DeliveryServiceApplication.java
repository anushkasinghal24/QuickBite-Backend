package com.quickbite.delivery_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * DeliveryServiceApplication
 *
 * PDF Section 4.7: Delivery-Agent-Service
 * Base Package: com.quickbite.delivery (PDF Section 5)
 *
 * Manages:
 *  - Agent registration + admin verification
 *  - Availability toggle (online/offline)
 *  - Live GPS location updates (real-time tracking)
 *  - Order assignment + completion
 *  - Earnings summary + ratings
 *
 * Port: 8087 (as per api-gateway route: lb://delivery-service → /api/v1/agents/**)
 */
@SpringBootApplication
@EnableDiscoveryClient      // registers with Eureka Server
@EnableFeignClients         // enables Feign for order-service, notification-service, auth-service calls
public class DeliveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }
}
