package com.quickbite.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Restaurant Entity
 *
 * PDF Section 4.2 ke anusar:
 * restaurantId, ownerId, name, description, cuisine, address, city,
 * latitude, longitude, phone, avgRating, isOpen, isApproved,
 * deliveryRadius, minOrderAmount, costForTwo, estimatedDeliveryMin
 *
 * Serializable isliye hai kyunki Redis caching mein save hoga.
 *
 * Relations:
 *  - One Restaurant -> Many MenuCategories (cascade)
 *  - avgRating -> updated by review-service via PUT /restaurants/{id}/rating
 */
@Entity
@Table(name = "restaurants", indexes = {
        @Index(name = "idx_owner_id",    columnList = "owner_id"),
        @Index(name = "idx_city",        columnList = "city"),
        @Index(name = "idx_cuisine",     columnList = "cuisine"),
        @Index(name = "idx_is_approved", columnList = "approval_status"),
        @Index(name = "idx_lat_lng",     columnList = "latitude, longitude")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Long restaurantId;

    /**
     * ownerId = userId from auth-service (ROLE_OWNER).
     * We don't FK to another DB — microservices pattern.
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "cuisine", nullable = false, length = 100)
    private String cuisine;

    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    /** GPS coordinates for Haversine geo-proximity search */
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    /** Image URL stored on AWS S3 / local path */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Average food rating — updated by review-service after each review submission */
    @Column(name = "avg_rating")
    @Builder.Default
    private Double avgRating = 0.0;

    /** Is the restaurant currently open for orders? (owner toggles) */
    @Column(name = "is_open")
    @Builder.Default
    private Boolean isOpen = false;

    /**
     * Admin must approve before restaurant is visible to customers.
     * Values: PENDING / APPROVED / REJECTED
     */
    @Column(name = "approval_status", length = 20)
    @Builder.Default
    private String approvalStatus = "PENDING";

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    /** Delivery radius in km — Haversine filter uses this */
    @Column(name = "delivery_radius")
    @Builder.Default
    private Double deliveryRadius = 5.0;

    @Column(name = "min_order_amount")
    @Builder.Default
    private Double minOrderAmount = 0.0;

    /**
     * Estimated cost for two people.
     * Used by customer-facing filters and list cards.
     */
    @Column(name = "cost_for_two")
    @Builder.Default
    private Double costForTwo = 0.0;

    /** Estimated delivery time in minutes shown on UI */
    @Column(name = "estimated_delivery_min")
    @Builder.Default
    private Integer estimatedDeliveryMin = 30;

    @Column(name = "opening_time", length = 10)
    private String openingTime;   // e.g. "09:00"

    @Column(name = "closing_time", length = 10)
    private String closingTime;   // e.g. "23:00"

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * One-to-Many with MenuCategory.
     * cascade = ALL: restaurant delete hone par categories bhi delete.
     * orphanRemoval = true: category remove karne par DB se bhi hata do.
     */
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MenuCategory> menuCategories = new ArrayList<>();

    // ===== Helper Methods =====

    public void addMenuCategory(MenuCategory category) {
        menuCategories.add(category);
        category.setRestaurant(this);
    }

    public void removeMenuCategory(MenuCategory category) {
        menuCategories.remove(category);
        category.setRestaurant(null);
    }
}
