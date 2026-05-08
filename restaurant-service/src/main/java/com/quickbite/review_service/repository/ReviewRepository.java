package com.quickbite.review_service.repository;

import com.quickbite.review_service.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ReviewRepository
 *
 * PDF Section 4.8 EXACT methods:
 *  findByRestaurantId(), findByCustomerId(), findByOrderId(), findByAgentId(),
 *  avgFoodRatingByRestaurantId(), avgDeliveryRatingByAgentId(),
 *  countByRestaurantId(), existsByOrderId()
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // ── PDF exact methods ─────────────────────────────────────────────────────

    List<Review> findByRestaurantId(Integer restaurantId);

    List<Review> findByCustomerId(Integer customerId);

    Optional<Review> findByOrderId(Integer orderId);

    List<Review> findByAgentId(Integer agentId);

    long countByRestaurantId(Integer restaurantId);

    boolean existsByOrderId(Integer orderId);

    // ── Average rating queries (PDF Section 4.8) ──────────────────────────────

    /**
     * Compute average food rating for a restaurant.
     * Called after every new/updated review to push back to restaurant-service.
     */
    @Query("SELECT AVG(r.foodRating) FROM Review r WHERE r.restaurantId = :restaurantId AND r.isVerified = true")
    Double avgFoodRatingByRestaurantId(@Param("restaurantId") Integer restaurantId);

    /**
     * Compute average delivery rating for an agent.
     * Called after every new/updated review to push back to delivery-service.
     */
    @Query("SELECT AVG(r.deliveryRating) FROM Review r WHERE r.agentId = :agentId AND r.deliveryRating IS NOT NULL AND r.isVerified = true")
    Double avgDeliveryRatingByAgentId(@Param("agentId") Integer agentId);

    // ── Moderation queries (PDF Section 2.5) ──────────────────────────────────

    /** Admin: get all flagged reviews awaiting moderation */
    List<Review> findByIsFlaggedTrue();

    /** Admin: get unverified reviews */
    List<Review> findByIsVerifiedFalse();

    /** Normalize legacy data so admin dashboard does not show stale pending reviews */
    @Modifying
    @Transactional
    @Query("update Review r set r.isVerified = true where r.isVerified = false")
    int markAllAsVerified();

    // ── Analytics queries ─────────────────────────────────────────────────────

    /** Count reviews by agent */
    long countByAgentId(Integer agentId);

    /** Top-rated reviews for a restaurant (for display) */
    List<Review> findByRestaurantIdAndIsVerifiedTrueOrderByFoodRatingDesc(Integer restaurantId);

    /** Recent reviews for a restaurant */
    List<Review> findTop10ByRestaurantIdAndIsVerifiedTrueOrderByCreatedAtDesc(Integer restaurantId);

    /** All verified reviews for a customer */
    List<Review> findByCustomerIdAndIsVerifiedTrue(Integer customerId);

    /** Check if customer already reviewed this restaurant (via order) */
    boolean existsByCustomerIdAndRestaurantId(Integer customerId, Integer restaurantId);
}
