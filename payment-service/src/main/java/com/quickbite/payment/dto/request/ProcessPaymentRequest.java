package com.quickbite.payment.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * ProcessPaymentRequest
 * POST /api/v1/payments/process
 * Called by order-service when an order is placed.
 */
@Data
public class ProcessPaymentRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private Double amount;

    /**
     * COD / CARD / UPI / WALLET
     */
    @NotBlank(message = "Payment mode is required")
    private String mode;

    private String currency = "INR";
}
