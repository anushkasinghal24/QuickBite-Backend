package com.quickbite.delivery_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Minimal order payload from order-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDetailsDTO {
    private Integer orderId;
    private Integer customerId;
    private String customerName;
    private Integer restaurantId;
    private String restaurantName;
    private Integer deliveryAgentId;
    private String orderStatus;
    private String deliveryAddress;
    private LocalDateTime orderDate;
    private LocalDateTime estimatedDelivery;
    private Double finalAmount;
    private String modeOfPayment;
    private Integer itemCount;
}
