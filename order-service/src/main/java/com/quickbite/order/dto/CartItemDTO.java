package com.quickbite.order.dto;

import lombok.*;

/**
 * CartItemDTO
 *
 * Mirrors CartItemResponse from cart-service.
 * Used when order-service snapshots cart items into OrderItems on order placement.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {

    private int itemId;
    private int menuItemId;
    private String name;
    private double price;
    private int quantity;
    private double lineTotal;
    private String customization;
    private boolean veg;
    private String imageUrl;
}
