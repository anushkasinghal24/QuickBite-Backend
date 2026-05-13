package com.quickbite.review_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for submitting a new review.
 *
 * PDF Section 2.2: "Rate restaurants (food quality) and delivery agents
 *                   (delivery experience) after order completion."
 * PDF Section 3.2: "Submit food rating and delivery experience rating after completion"
 *
 * customerId is extracted from JWT — NOT sent in request body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddReviewRequest {

    @NotNull(message = "Order ID is required")
    private Integer orderId;

    @NotNull(message = "Restaurant ID is required")
    private Integer restaurantId;

    /** Agent ID — nullable (COD orders may not have agent) */
    private Integer agentId;

    @NotNull(message = "Food rating is required")
    @Min(value = 1, message = "Food rating must be at least 1")
    @Max(value = 5, message = "Food rating must be at most 5")
    private Integer foodRating;

    /** Delivery rating — nullable when agentId is null */
    @Min(value = 1, message = "Delivery rating must be at least 1")
    @Max(value = 5, message = "Delivery rating must be at most 5")
    private Integer deliveryRating;

    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    private String comment;
}
