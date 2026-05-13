package com.quickbite.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Payment Entity â€” PDF Section 4.6
 *
 * One Payment per Order.
 * Fields: paymentId, orderId, customerId, amount, status, mode,
 *         transactionId, currency, paidAt, refundedAt
 *
 * Status lifecycle:
 *   PENDING â†’ PAID (on successful payment)
 *   PAID    â†’ REFUNDED (on order cancellation)
 *   PENDING â†’ FAILED (on payment gateway failure)
 *
 * Called by:
 *   - order-service: POST /api/v1/payments/process (when order is placed)
 *   - order-service: POST /api/v1/payments/{id}/refund (on cancellation)
 *   - customer: GET /api/v1/payments/customer/{id}
 *   - admin: GET /api/v1/payments/all
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_order_id",    columnList = "order_id"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_status",      columnList = "status"),
        @Index(name = "idx_txn_id",      columnList = "transaction_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    /** orderId from order-service â€” no FK (microservices pattern) */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** customerId from auth-service */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    /**
     * PENDING / PAID / REFUNDED / FAILED
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /**
     * COD / CARD / UPI / WALLET
     */
    @Column(name = "mode", nullable = false, length = 20)
    private String mode;

    /** Auto-generated unique transaction ID: QB-TXN-{UUID} */
    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @Column(name = "gateway_order_id", length = 100)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "refund_transaction_id", length = 100)
    private String refundTransactionId;

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
}
