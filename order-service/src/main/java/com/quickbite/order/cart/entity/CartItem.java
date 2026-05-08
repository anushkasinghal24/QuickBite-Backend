package com.quickbite.order.cart.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * CartItem Entity
 *
 * As per PDF (Section 4.4):
 *  - itemId, menuItemId, name, price, quantity, customization
 *  - price snapshot at time of add (not live menu price â€” avoids price drift during session)
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "cart")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private int itemId;

    /** FK to MenuItem in menu-service (cross-service reference, not a JPA FK) */
    @Column(name = "menu_item_id", nullable = false)
    private int menuItemId;

    /** Snapshot of item name at add time */
    @Column(name = "name", nullable = false)
    private String name;

    /** Price snapshot at the time item was added â€” PDF says "price snapshot at time of add" */
    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Optional special instructions / customisation (e.g. "extra spicy", "no onion") */
    @Column(name = "customization")
    private String customization;

    /** isVeg flag for display (snapshot from menu-service) */
    @Column(name = "is_veg")
    private boolean veg;

    /** Image URL snapshot for cart display */
    @Column(name = "image_url")
    private String imageUrl;

    /** Many CartItems belong to one Cart */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // â”€â”€ Convenience â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public double getLineTotal() {
        return price * quantity;
    }
}
