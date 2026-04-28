package com.quickbite.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private Long paymentId;
    private Long orderId;
    private Long customerId;
    private Double amount;
    private String status;
    private String mode;
    private String transactionId;
    private String currency;
    private String refundTransactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
}
