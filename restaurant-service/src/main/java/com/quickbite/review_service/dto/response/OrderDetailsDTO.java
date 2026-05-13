package com.quickbite.review_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Minimal order payload fetched from order-service for review validation.
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
    private Integer restaurantId;
    private Integer deliveryAgentId;
    private String orderStatus;
    private String restaurantName;
    private String customerName;
    private LocalDateTime orderDate;
}
