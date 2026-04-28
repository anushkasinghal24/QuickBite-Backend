package com.quickbite.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * QuickBite â€” Payment Service
 *
 * Responsibilities (PDF Section 4.6):
 *  - Process order payments (COD / CARD / UPI / WALLET)
 *  - Manage customer e-wallet (deposit / debit)
 *  - Wallet balance validation (no negative balance)
 *  - Refunds on order cancellation
 *  - Full audit trail via WalletStatement records
 *  - Transaction history for customers and admin
 *
 * Port   : 8084
 * DB     : quickbite_payment (MySQL)
 * Feign  : order-service, notification-service
 */
@SpringBootApplication
@org.springframework.context.annotation.ComponentScan(basePackages = "com.quickbite.payment")
@EnableDiscoveryClient
@EnableAsync
@EntityScan(basePackages = "com.quickbite")
@EnableJpaRepositories(basePackages = "com.quickbite")
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
