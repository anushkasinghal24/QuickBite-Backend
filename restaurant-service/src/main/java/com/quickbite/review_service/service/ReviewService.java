package com.quickbite.review_service.service;

import com.quickbite.review_service.dto.request.*;
import com.quickbite.review_service.dto.response.*;

import java.util.List;

/**
 * ReviewService Interface
 *
 * PDF Section 4.8 EXACT methods:
 *  addReview(), getByRestaurant(), getByCustomer(), getByOrder(), getByAgent(),
 *  updateReview(), deleteReview(), getAvgFoodRating(), getAvgDeliveryRating(), getAllReviews()
 */
public interface ReviewService {

    /**
     * Customer submits a review after order completion.
     * PDF: "One review per order, enforced by unique constraint on orderId."
     * After saving:
     *  → recompute avgFoodRating → push to restaurant-service
     *  → recompute avgDeliveryRating → push to delivery-service
     *  → send notification to restaurant owner
     */
    ReviewResponse addReview(Integer customerId, AddReviewRequest request);

    /** Get all reviews for a restaurant */
    List<ReviewResponse> getByRestaurant(Integer restaurantId);

    /** Get all reviews by a customer */
    List<ReviewResponse> getByCustomer(Integer customerId);

    /** Get the single review for an order */
    ReviewResponse getByOrder(Integer orderId);

    /** Get all reviews for a delivery agent */
    List<ReviewResponse> getByAgent(Integer agentId);

    /**
     * Customer updates their own review.
     * Only the reviewer (customerId) can update their review.
     */
    ReviewResponse updateReview(Integer reviewId, Integer customerId, UpdateReviewRequest request);

    /**
     * Delete review.
     * ADMIN can delete any review (moderation).
     * CUSTOMER can delete their own review.
     * PDF Section 2.5: "Remove fraudulent or inappropriate content."
     */
    void deleteReview(Integer reviewId);

    /**
     * Get average food rating for a restaurant.
     * PDF: avgFoodRatingByRestaurantId()
     */
    Double getAvgFoodRating(Integer restaurantId);

    /**
     * Get average delivery rating for an agent.
     * PDF: avgDeliveryRatingByAgentId()
     */
    Double getAvgDeliveryRating(Integer agentId);

    /**
     * Get ALL reviews — Admin use.
     * PDF: getAllReviews()
     */
    List<ReviewResponse> getAllReviews();

    /**
     * Flag a review for admin moderation.
     * PDF Section 2.3: "Flag inappropriate reviews for moderation."
     * Called by OWNER for their restaurant reviews, or by ADMIN.
     */
    ReviewResponse flagReview(Integer reviewId, FlagReviewRequest request);

    /**
     * Admin: verify/approve a flagged review (mark as clean).
     * PDF Section 2.5: Admin moderates reviews.
     */
    ReviewResponse verifyReview(Integer reviewId);

    /**
     * Get rating summary for a restaurant (avg + total).
     */
    RatingAverageResponse getRestaurantRatingSummary(Integer restaurantId);

    /**
     * Get rating summary for an agent (avg + total).
     */
    RatingAverageResponse getAgentRatingSummary(Integer agentId);

    /**
     * Admin: get all flagged reviews awaiting moderation.
     */
    List<ReviewResponse> getFlaggedReviews();
}
