package com.quickbite.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MenuItem Entity
 *
 * PDF Section 4.3:
 * itemId, restaurantId, categoryId, name, description, price, discountedPrice,
 * imageUrl, isVeg, isAvailable, rating, calories, tags
 *
 * cart-service will copy: itemId, name, price at time of addToCart (price snapshot).
 * order-service will copy: name, price, quantity as OrderItem snapshot.
 */
@Entity
@Table(name = "menu_items", indexes = {
        @Index(name = "idx_item_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_item_category_id",   columnList = "category_id"),
        @Index(name = "idx_is_veg",             columnList = "is_veg"),
        @Index(name = "idx_is_available",       columnList = "is_available")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    /** Redundant FK for direct queries (menu-service pattern from PDF) */
    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 400)
    private String description;

    @Column(name = "price", nullable = false)
    private Double price;

    /** If discountedPrice < price, UI shows strikethrough original price */
    @Column(name = "discounted_price")
    private Double discountedPrice;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** true = Veg, false = Non-Veg — shown with green/red dot on UI */
    @Column(name = "is_veg")
    @Builder.Default
    private Boolean isVeg = true;

    /**
     * Owner can toggle this in real time.
     * cart-service validates availability before adding to cart.
     */
    @Column(name = "is_available")
    @Builder.Default
    private Boolean isAvailable = true;

    /** Average item rating (optional — can be computed from reviews) */
    @Column(name = "rating")
    @Builder.Default
    private Double rating = 0.0;

    @Column(name = "calories")
    private Integer calories;

    /** Comma-separated tags: e.g. "spicy,bestseller,new" */
    @Column(name = "tags", length = 200)
    private String tags;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /** ManyToOne back to MenuCategory */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MenuCategory menuCategory;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== Convenience getter for cart-service =====
    public Double getEffectivePrice() {
        return (discountedPrice != null && discountedPrice > 0 && discountedPrice < price)
               ? discountedPrice : price;
    }
}
