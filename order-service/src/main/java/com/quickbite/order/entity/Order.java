package com.quickbite.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Entity
 *
 * As per PDF Section 4.5:
 *  orderId, customerId, restaurantId, deliveryAgentId,
 *  totalAmount, discount, finalAmount, modeOfPayment,
 *  orderStatus (PLACED/CONFIRMED/PREPARING/PICKED_UP/DELIVERED/CANCELLED),
 *  orderDate, estimatedDelivery, deliveryAddress, specialInstructions
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "orderItems")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private int orderId;

    /** FK reference to Customer (auth-service User with role=CUSTOMER) */
    @Column(name = "customer_id", nullable = false)
    private int customerId;

    /** FK reference to Restaurant (restaurant-service) */
    @Column(name = "restaurant_id", nullable = false)
    private int restaurantId;

    /**
     * FK reference to Delivery Agent (delivery-service).
     * NULL until order-service assigns an agent after CONFIRMED.
     */
    @Column(name = "delivery_agent_id")
    private Integer deliveryAgentId;

    /** Sum of all OrderItem (price Ã— quantity) before discount */
    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    /** Discount amount (from promo code applied at cart level) */
    @Column(name = "discount")
    private double discount;

    /** finalAmount = totalAmount - discount */
    @Column(name = "final_amount", nullable = false)
    private double finalAmount;

    /**
     * Payment mode chosen at checkout.
     * Values: COD, CARD, UPI, WALLET
     * As per PDF Section 2.6 & 4.5
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode_of_payment", nullable = false, length = 10)
    private PaymentMode modeOfPayment;

    /**
     * Order lifecycle status.
     * PLACED â†’ CONFIRMED â†’ PREPARING â†’ PICKED_UP â†’ DELIVERED
     * Also: CANCELLED
     * As per PDF Section 4.5 & 8 (Glossary)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private OrderStatus orderStatus;

    /** Timestamp when order was placed */
    @CreationTimestamp
    @Column(name = "order_date", updatable = false)
    private LocalDateTime orderDate;

    /** Estimated delivery time (set by restaurant-service or admin) */
    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;

    /** Full delivery address (snapshot at time of order) */
    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    /** Optional special instructions from customer (e.g. "no spice", "ring bell") */
    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    /**
     * Snapshot of customer's name at time of order (for display / history).
     * Denormalized to avoid cross-service call on every history fetch.
     */
    @Column(name = "customer_name")
    private String customerName;

    /**
     * Snapshot of restaurant name at time of order.
     * Denormalized so order history shows correct name even if restaurant renames.
     */
    @Column(name = "restaurant_name")
    private String restaurantName;

    /**
     * One-to-Many: An Order contains multiple OrderItems (snapshots of CartItems).
     * CascadeType.ALL â€” items are saved/deleted with the order.
     * As per PDF: OrderItem is an "immutable snapshot of CartItem"
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems = new ArrayList<>();

    // â”€â”€ Enums â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public enum OrderStatus {
        PLACED,
        CONFIRMED,
        PREPARING,
        PICKED_UP,
        DELIVERED,
        CANCELLED
    }

    public enum PaymentMode {
        COD,
        CARD,
        UPI,
        WALLET
    }

    // â”€â”€ Convenience â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Recalculate finalAmount from totalAmount and discount */
    public void recalculateFinalAmount() {
        this.finalAmount = this.totalAmount - this.discount;
        if (this.finalAmount < 0) this.finalAmount = 0;
    }

    /** Check if order can still be cancelled (only before PREPARING starts) */
    public boolean isCancellable() {
        return orderStatus == OrderStatus.PLACED || orderStatus == OrderStatus.CONFIRMED;
    }

    /** Check if order is in a terminal state */
    public boolean isTerminal() {
        return orderStatus == OrderStatus.DELIVERED || orderStatus == OrderStatus.CANCELLED;
    }
}
