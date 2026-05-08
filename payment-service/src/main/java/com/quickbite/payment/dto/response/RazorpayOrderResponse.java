package com.quickbite.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RazorpayOrderResponse {
    private String keyId;
    private String razorpayOrderId;
    private Long orderId;
    private Long customerId;
    private Double amount;
    private String currency;
    private String status;
    private String receipt;
}
