package com.quickbite.order.dto;

import com.quickbite.order.entity.Order.OrderStatus;
import com.quickbite.order.entity.Order.PaymentMode;
import lombok.*;

import java.time.LocalDateTime;

/**
 * OrderSummaryDTO
 *
 * Lightweight order card used for:
 *  - Customer order history list (PDF Section 2.2: View complete order history)
 *  - Restaurant incoming orders queue
 *  - Admin order monitoring
 *
 * Does NOT include full item list (use OrderResponse for that).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryDTO {

    private int orderId;
    private int customerId;
    private String customerName;
    private int restaurantId;
    private String restaurantName;
    private Integer deliveryAgentId;
    private String deliveryAddress;
    private OrderStatus orderStatus;
    private double finalAmount;
    private PaymentMode modeOfPayment;
    private LocalDateTime orderDate;
    private int itemCount;
    private boolean cancellable;
}
