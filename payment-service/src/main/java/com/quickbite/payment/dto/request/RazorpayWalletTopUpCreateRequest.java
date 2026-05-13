package com.quickbite.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RazorpayWalletTopUpCreateRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least 1")
    private Double amount;

    private String currency = "INR";
}
