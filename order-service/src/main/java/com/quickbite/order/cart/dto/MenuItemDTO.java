package com.quickbite.order.cart.dto;

import lombok.*;

/**
 * DTO used when cart-service calls menu-service via Feign
 * to fetch item details for snapshot storage.
 *
 * Fields match MenuItem entity in menu-service (Section 4.3 of PDF).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemDTO {

    private int itemId;
    private int restaurantId;
    private int categoryId;
    private String name;
    private String description;
    private double price;
    private double discountedPrice;
    private String imageUrl;
    private boolean veg;
    private boolean available;
    private double rating;
    private int calories;
    private String tags;

    /** Returns effective price (discounted if available, else regular) */
    public double getEffectivePrice() {
        return discountedPrice > 0 && discountedPrice < price ? discountedPrice : price;
    }
}
