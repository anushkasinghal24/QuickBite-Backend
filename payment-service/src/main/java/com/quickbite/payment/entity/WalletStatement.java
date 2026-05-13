package com.quickbite.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * WalletStatement Entity â€” PDF Section 4.6
 *
 * Every wallet operation (deposit or debit) generates one statement record.
 * This gives customers a full transaction statement history.
 *
 * type: CREDIT (money added / refund) or DEBIT (payment made)
 *
 * Used for:
 *   - Customer: GET /api/v1/wallet/statements/{customerId}
 *   - Admin: full audit trail
 */
@Entity
@Table(name = "wallet_statements", indexes = {
        @Index(name = "idx_stmt_wallet_id",   columnList = "wallet_id"),
        @Index(name = "idx_stmt_customer_id", columnList = "customer_id"),
        @Index(name = "idx_stmt_type",        columnList = "type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statement_id")
    private Long statementId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    /**
     * CREDIT â€” deposit / refund received
     * DEBIT  â€” payment made for order
     */
    @Column(name = "type", nullable = false, length = 10)
    private String type;

    @Column(name = "description", length = 300)
    private String description;

    /** Balance after this transaction (running total) */
    @Column(name = "closing_balance", nullable = false)
    private Double closingBalance;

    /** Related orderId or paymentId for cross-reference */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Wallet wallet;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
