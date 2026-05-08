package com.quickbite.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request payload used when assigning an order to a delivery agent.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignOrderRequest {

    @NotNull(message = "Order ID is required")
    private Integer orderId;

    private Double restaurantLatitude;
    private Double restaurantLongitude;
}
