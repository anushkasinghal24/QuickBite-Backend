package com.quickbite.review_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Review Entity
 *
 * PDF Section 4.8 — EXACT fields:
 *  reviewId, orderId (unique), customerId, restaurantId, agentId,
 *  foodRating (1-5), deliveryRating (1-5), comment, reviewDate, isVerified
 *
 * Key business rules (from PDF):
 *  1. One review per order → unique constraint on orderId
 *  2. Dual ratings: foodRating for restaurant + deliveryRating for agent
 *  3. Average ratings pushed back to restaurant-service and delivery-service
 *  4. Admin can moderate (isVerified flag, flag for moderation)
 */
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "order_id", name = "uq_review_order")
    },
    indexes = {
        @Index(columnList = "restaurant_id", name = "idx_review_restaurant"),
        @Index(columnList = "customer_id",   name = "idx_review_customer"),
        @Index(columnList = "agent_id",      name = "idx_review_agent"),
        @Index(columnList = "order_id",      name = "idx_review_order"),
        @Index(columnList = "is_flagged",    name = "idx_review_flagged")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    /**
     * Unique per order — one review per completed order.
     * PDF: "One review is permitted per order, enforced by a unique constraint on orderId."
     */
    @Column(name = "order_id", nullable = false, unique = true)
    private Integer orderId;

    /**
     * FK to auth-service User (role = CUSTOMER).
     * Cross-service reference.
     */
    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    /**
     * FK to restaurant-service Restaurant.
     * review-service pushes avgFoodRating back to restaurant-service after each review.
     */
    @Column(name = "restaurant_id", nullable = false)
    private Integer restaurantId;

    /**
     * FK to delivery-service DeliveryAgent.
     * review-service pushes avgDeliveryRating back to delivery-service after each review.
     * Nullable: COD orders without agent tracking may not have agentId.
     */
    @Column(name = "agent_id")
    private Integer agentId;

    /**
     * Food quality rating 1-5 stars.
     * Pushed to restaurant-service via Feign: PUT /api/v1/restaurants/{id}/rating
     * PDF: "food quality rating for the restaurant"
     */
    @Column(name = "food_rating", nullable = false)
    private Integer foodRating;

    /**
     * Delivery experience rating 1-5 stars.
     * Pushed to delivery-service via Feign: PUT /api/v1/agents/{id}/rating
     * PDF: "delivery experience rating for the agent"
     * Nullable: if no agent was assigned (e.g. pickup order)
     */
    @Column(name = "delivery_rating")
    private Integer deliveryRating;

    /** Customer's written review comment */
    @Column(name = "comment", length = 1000)
    private String comment;

    /** Date the review was submitted — PDF: reviewDate:LocalDate */
    @Column(name = "review_date")
    private LocalDate reviewDate;

    /**
     * Admin verification flag.
     * PDF: isVerified boolean
     * false = pending moderation / flagged
     * true  = verified / clean
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = true;

    /**
     * Flagged for admin moderation.
     * PDF Section 2.3: "Flag inappropriate reviews for moderation"
     * PDF Section 2.5: "Remove fraudulent or inappropriate content"
     */
    @Column(name = "is_flagged", nullable = false)
    @Builder.Default
    private Boolean isFlagged = false;

    /** Reason given when flagging (by restaurant owner or system) */
    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
