package com.quickbite.order.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * OrderItem Entity
 *
 * As per PDF Section 4.5:
 *  "OrderItem â€” immutable snapshot of CartItem saved to the Order on placement
 *   for historical accuracy"
 *
 * Fields: orderItemId, orderId, menuItemId, name, price, quantity, customization
 *
 * NOTE: Once saved, OrderItems are NEVER modified. They form an immutable
 * receipt of what the customer ordered at the prices they paid.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "order")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private int orderItemId;

    /** Cross-service reference to MenuItem in menu-service (NOT a JPA FK) */
    @Column(name = "menu_item_id", nullable = false)
    private int menuItemId;

    /** Snapshot of item name at time of order (even if restaurant renames item later) */
    @Column(name = "name", nullable = false)
    private String name;

    /** Price snapshot â€” the actual price the customer paid per unit */
    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Optional customization notes (e.g. "extra spicy", "no onion") */
    @Column(name = "customization")
    private String customization;

    /** isVeg snapshot for display on order history */
    @Column(name = "is_veg")
    private boolean veg;

    /** Image URL snapshot for order history display */
    @Column(name = "image_url")
    private String imageUrl;

    /** Many OrderItems belong to one Order */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // â”€â”€ Convenience â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Line total = price Ã— quantity */
    public double getLineTotal() {
        return price * quantity;
    }
}
