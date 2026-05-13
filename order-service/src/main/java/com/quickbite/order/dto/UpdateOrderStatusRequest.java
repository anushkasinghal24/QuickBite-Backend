package com.quickbite.order.dto;

import com.quickbite.order.entity.Order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * UpdateOrderStatusRequest
 *
 * Used by Restaurant Owner and Delivery Agent to advance order status.
 * Restaurant Owner: PLACED -> CONFIRMED -> PREPARING -> READY_TO_PICK_UP
 * Delivery Agent:   READY_TO_PICK_UP -> PICKED_UP -> DELIVERED
 * Admin:            Can update to any status
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "New order status is required")
    private OrderStatus status;
}
