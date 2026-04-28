package com.quickbite.restaurant.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * PUT /api/v1/restaurants/{id}/rating
 * Called internally by review-service (via Feign) after each review submission.
 * This is an INTERNAL endpoint — secured with service-to-service header.
 */
@Data
public class UpdateRatingRequest {

    @NotNull
    @DecimalMin("0.0") @DecimalMax("5.0")
    private Double avgRating;

    @NotNull @Min(0)
    private Integer totalReviews;
}
