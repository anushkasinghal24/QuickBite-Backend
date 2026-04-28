package com.quickbite.order.dto;

import lombok.*;

/**
 * RestaurantDTO
 *
 * Minimal data fetched from restaurant-service via Feign
 * when placing an order (to validate restaurant is open/approved
 * and to snapshot restaurant name).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDTO {

    private int restaurantId;
    private int ownerId;
    private String name;
    private String cuisine;
    private boolean open;
    private boolean approved;
    private double minOrderAmount;
    private int estimatedDeliveryMin;
}
