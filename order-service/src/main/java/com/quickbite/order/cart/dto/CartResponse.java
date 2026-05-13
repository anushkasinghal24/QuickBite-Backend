package com.quickbite.order.cart.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private int cartId;
    private int customerId;
    private Integer restaurantId;
    private String restaurantName;       // populated via restaurant-service later
    private List<CartItemResponse> items;
    private int itemCount;
    private double subtotal;             // before discount
    private double discountAmount;
    private double totalPrice;           // after discount
    private String promoCode;
    private boolean empty;
}
