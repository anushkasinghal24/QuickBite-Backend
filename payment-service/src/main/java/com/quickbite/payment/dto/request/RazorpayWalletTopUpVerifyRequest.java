package com.quickbite.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RazorpayWalletTopUpVerifyRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "amount is required")
    private Double amount;

    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;

    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;

    private String currency = "INR";
}
