package com.quickbite.order.cart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Cart Entity
 *
 * As per PDF (Section 4.4):
 *  - cartId, customerId, restaurantId, totalPrice
 *  - contains 0..* CartItems
 *  - One active cart per customer
 *  - Cart is locked to a single restaurant (enforces single-restaurant ordering rule)
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "items")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private int cartId;

    /** FK reference to Customer (auth-service User with role=CUSTOMER) */
    @Column(name = "customer_id", nullable = false, unique = true)
    private int customerId;

    /**
     * Locked to a single restaurant.
     * If customer tries to add item from different restaurant â†’ prompt to clear cart first.
     * NULL when cart is empty.
     */
    @Column(name = "restaurant_id")
    private Integer restaurantId;

    /** Running total of all CartItem (price * quantity) after promo */
    @Column(name = "total_price")
    private double totalPrice;

    /** Applied promo code (if any) */
    @Column(name = "promo_code")
    private String promoCode;

    /** Discount amount deducted after promo */
    @Column(name = "discount_amount")
    private double discountAmount;

    /**
     * One-to-Many: A cart can have multiple CartItems.
     * CascadeType.ALL so that items are saved/deleted with the cart.
     * orphanRemoval ensures removed items are deleted from DB.
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>();

    // â”€â”€ Convenience helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Recompute totalPrice from items (before discount) */
    public void recalculateTotal() {
        double subtotal = items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
        this.totalPrice = subtotal - this.discountAmount;
        if (this.totalPrice < 0) this.totalPrice = 0;
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
