package com.quickbite.restaurant.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/** POST /api/v1/restaurants/{id}/categories */
@Data
public class AddCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 300)
    private String description;

    private String imageUrl;

    @Min(1)
    private Integer displayOrder = 1;
}
