package com.quickbite.delivery_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Minimal restaurant payload from restaurant-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestaurantDetailsDTO {
    private Long restaurantId;
    private Long ownerId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private Boolean isOpen;
    private String approvalStatus;
}
