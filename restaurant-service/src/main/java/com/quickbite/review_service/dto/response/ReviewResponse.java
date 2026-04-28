package com.quickbite.review_service.dto.response;

import com.quickbite.review_service.entity.Review;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full review response DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Integer reviewId;
    private Integer orderId;
    private Integer customerId;
    private String  customerName;    // enriched from auth-service (optional)
    private Integer restaurantId;
    private Integer agentId;
    private Integer foodRating;
    private Integer deliveryRating;
    private String  comment;
    private LocalDate reviewDate;
    private Boolean isVerified;
    private Boolean isFlagged;
    private String  flagReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Static factory to build from entity */
    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .orderId(review.getOrderId())
                .customerId(review.getCustomerId())
                .restaurantId(review.getRestaurantId())
                .agentId(review.getAgentId())
                .foodRating(review.getFoodRating())
                .deliveryRating(review.getDeliveryRating())
                .comment(review.getComment())
                .reviewDate(review.getReviewDate())
                .isVerified(review.getIsVerified())
                .isFlagged(review.getIsFlagged())
                .flagReason(review.getFlagReason())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
