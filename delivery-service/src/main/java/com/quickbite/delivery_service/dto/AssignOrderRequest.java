package com.quickbite.delivery_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Assign order to delivery agent.
 * Called by order-service (internally) when placing an order.
 * PDF Section 4.7: assignOrder()
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AssignOrderRequest {

    @NotNull(message = "Order ID is required")
    private Integer orderId;

    /** Restaurant coordinates — used to validate agent proximity */
    private Double restaurantLatitude;
    private Double restaurantLongitude;
}
