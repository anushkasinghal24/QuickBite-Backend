package com.quickbite.delivery_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Update agent's average rating.
 * Called INTERNALLY by review-service after customer submits delivery rating.
 * PDF Section 4.8: "Average ratings computed and pushed back to Delivery-Agent-Service"
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateRatingRequest {

    @NotNull(message = "Average rating is required")
    @DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Rating must be at most 5.0")
    private Double avgRating;
}
