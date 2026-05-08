package com.quickbite.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RazorpayVerifyPaymentRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;

    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;

    private Double amount;

    private String currency = "INR";
}
