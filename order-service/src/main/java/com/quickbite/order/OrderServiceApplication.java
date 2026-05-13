package com.quickbite.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * OrderServiceApplication
 *
 * Central orchestration service of the QuickBite platform.
 * As per PDF Section 4.5 â€” converts confirmed cart into an Order,
 * manages complete order lifecycle, calls Cart/Payment/Delivery services.
 *
 * Port: 8085
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.quickbite")
@EntityScan(basePackages = "com.quickbite")
@EnableJpaRepositories(basePackages = "com.quickbite")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
