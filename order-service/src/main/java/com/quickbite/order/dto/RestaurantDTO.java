package com.quickbite.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * RestaurantDTO
 *
 * Minimal data fetched from restaurant-service via Feign.
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
    @JsonProperty("isOpen")
    @JsonAlias({"isOpen", "open"})
    private boolean open;
    private boolean approved;
    private String approvalStatus;
    private Double latitude;
    private Double longitude;
    private Double deliveryRadius;
    private double minOrderAmount;
    private int estimatedDeliveryMin;
}
