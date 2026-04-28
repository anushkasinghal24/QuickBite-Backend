package com.quickbite.order.dto;

import com.quickbite.order.entity.Order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * UpdateOrderStatusRequest
 *
 * Used by Restaurant Owner and Delivery Agent to advance order status.
 * Restaurant Owner: PLACED â†’ CONFIRMED â†’ PREPARING â†’ READY(PICKED_UP)
 * Delivery Agent:   CONFIRMED â†’ PICKED_UP â†’ DELIVERED
 * Admin:            Can update to any status
 *
 * As per PDF Section 4.5 & 3.2 (Use Case: Update Order Status)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "New order status is required")
    private OrderStatus status;
}
