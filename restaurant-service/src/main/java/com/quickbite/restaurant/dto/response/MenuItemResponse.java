package com.quickbite.restaurant.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MenuItemResponse {
    private Long itemId;
    private Long restaurantId;
    private Long categoryId;
    private String name;
    private String description;
    private Double price;
    private Double discountedPrice;
    private Double effectivePrice;  // min(price, discountedPrice)
    private String imageUrl;
    private Boolean isVeg;
    private Boolean isAvailable;
    private Double rating;
    private Integer calories;
    private String tags;
}
