package com.quickbite.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Wallet Entity â€” PDF Section 4.6
 *
 * One Wallet per Customer.
 * Tracks running balance and contains list of WalletStatements (CREDIT/DEBIT).
 *
 * Balance validation rule:
 *   balance must NEVER go below 0 (enforced in service layer before debit).
 *
 * Operations:
 *   - deposit (add money via card/UPI)
 *   - debit   (pay for order)
 *   - credit  (refund credited back)
 */
@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallet_customer_id", columnList = "customer_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Column(name = "customer_id", nullable = false, unique = true)
    private Long customerId;

    @Column(name = "balance", nullable = false)
    @Builder.Default
    private Double balance = 0.0;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "INR";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** One Wallet -> Many WalletStatements */
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WalletStatement> statements = new ArrayList<>();

    // Helper
    public void addStatement(WalletStatement stmt) {
        statements.add(stmt);
        stmt.setWallet(this);
    }
}
