package com.quickbite.restaurant.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/** POST /api/v1/restaurants/{restaurantId}/categories/{categoryId}/items */
@Data
public class AddMenuItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(min = 2, max = 150)
    private String name;

    @Size(max = 400)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private Double price;

    @DecimalMin(value = "0.0")
    private Double discountedPrice;

    private String imageUrl;

    private Boolean isVeg = true;

    @Min(0) @Max(5000)
    private Integer calories;

    /** Comma-separated: "spicy,bestseller" */
    private String tags;
}
