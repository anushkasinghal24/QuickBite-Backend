package com.quickbite.order.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * CartItemHistory Entity
 *
 * Stores a permanent snapshot of cart items at checkout time so the cart
 * contents remain auditable even after the live cart is cleared.
 */
@Entity
@Table(name = "cart_item_history", indexes = {
        @Index(name = "idx_cart_history_order_id", columnList = "order_id"),
        @Index(name = "idx_cart_history_customer_id", columnList = "customer_id"),
        @Index(name = "idx_cart_history_cart_id", columnList = "cart_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "cart_id", nullable = false)
    private Integer cartId;

    @Column(name = "restaurant_id", nullable = false)
    private Integer restaurantId;

    @Column(name = "cart_item_id")
    private Integer cartItemId;

    @Column(name = "menu_item_id", nullable = false)
    private Integer menuItemId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "customization", length = 500)
    private String customization;

    @Column(name = "is_veg")
    private Boolean veg;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "line_total", nullable = false)
    private Double lineTotal;

    @CreationTimestamp
    @Column(name = "archived_at", updatable = false)
    private LocalDateTime archivedAt;
}
