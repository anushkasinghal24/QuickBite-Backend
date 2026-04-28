package com.quickbite.restaurant.dto.response;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MenuCategoryResponse {
    private Long categoryId;
    private Long restaurantId;
    private String name;
    private String description;
    private String imageUrl;
    private Integer displayOrder;
    private Boolean isActive;
    private List<MenuItemResponse> menuItems;
}
