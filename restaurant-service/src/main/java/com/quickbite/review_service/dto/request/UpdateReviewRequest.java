package com.quickbite.review_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Update existing review — customer can update their own review */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateReviewRequest {

    @Min(value = 1, message = "Food rating must be at least 1")
    @Max(value = 5, message = "Food rating must be at most 5")
    private Integer foodRating;

    @Min(value = 1, message = "Delivery rating must be at least 1")
    @Max(value = 5, message = "Delivery rating must be at most 5")
    private Integer deliveryRating;

    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    private String comment;
}
