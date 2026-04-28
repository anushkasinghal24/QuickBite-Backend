package com.quickbite.review_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Flag a review for admin moderation.
 * PDF Section 2.3: "Flag inappropriate reviews for moderation"
 * Can be called by OWNER (for their restaurant) or ADMIN.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class FlagReviewRequest {

    @NotBlank(message = "Flag reason is required")
    @Size(max = 500, message = "Reason max 500 characters")
    private String reason;
}
