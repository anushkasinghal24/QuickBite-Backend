package com.quickbite.order.cart.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

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
