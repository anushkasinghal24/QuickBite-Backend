package com.quickbite.order.dto;

import lombok.*;

import java.util.List;

/**
 * CartDTO
 *
 * Mirrors CartResponse from cart-service.
 * Order-service fetches this from cart-service via Feign
 * when a customer places an order (to snapshot items into OrderItems).
 *
 * Must match the structure of CartResponse in cart-service exactly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {

    private int cartId;
    private int customerId;
    private Integer restaurantId;
    private String restaurantName;
    private List<CartItemDTO> items;
    private int itemCount;
    private double subtotal;
    private double discountAmount;
    private double totalPrice;
    private String promoCode;
    private boolean empty;
}
