package com.quickbite.payment.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/** POST /api/v1/wallet/topup â€” customer adds money to wallet */
@Data
public class WalletTopUpRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull
    @DecimalMin(value = "1.0", message = "Minimum top-up amount is â‚¹1")
    @DecimalMax(value = "100000.0", message = "Maximum single top-up is â‚¹1,00,000")
    private Double amount;

    /** CARD or UPI â€” source of money */
    @NotBlank
    private String sourceMode;

    /** Optional: external transaction ID from payment gateway (Razorpay/Stripe) */
    private String gatewayTransactionId;
}
