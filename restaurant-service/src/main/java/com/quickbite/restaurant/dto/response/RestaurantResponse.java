package com.quickbite.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RestaurantResponse — returned to clients (customers, admin).
 * Never expose internal DB IDs directly — use response DTOs.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RestaurantResponse {
    private Long restaurantId;
    private Long ownerId;
    private String name;
    private String description;
    private String cuisine;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String email;
    private String imageUrl;
    private Double avgRating;
    private Boolean isOpen;
    private String approvalStatus;
    private Boolean approved;
    private Double deliveryRadius;
    private Double minOrderAmount;
    private Double costForTwo;
    private Integer estimatedDeliveryMin;
    private String openingTime;
    private String closingTime;
    private Integer totalReviews;
    private LocalDateTime createdAt;
    /** Populated only when fetching full restaurant detail (not in list) */
    private List<MenuCategoryResponse> menuCategories;
}
