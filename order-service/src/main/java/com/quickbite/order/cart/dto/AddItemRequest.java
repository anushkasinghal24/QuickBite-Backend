package com.quickbite.order.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for adding an item to the cart.
 * Cart-service will call menu-service to fetch item details (name, price, isVeg, imageUrl)
 * and snapshot them into CartItem.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddItemRequest {

    @NotNull(message = "menuItemId is required")
    private Integer menuItemId;

    @NotNull(message = "restaurantId is required")
    private Integer restaurantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    /** Optional: special instructions / customisation */
    private String customization;
}
