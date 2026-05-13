package com.quickbite.delivery_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Persistent delivery history snapshot for an agent.
 * This stores the completed delivery details so the agent dashboard can
 * render a real history instead of relying only on the live current-order state.
 */
@Entity
@Table(
    name = "delivery_history",
    indexes = {
        @Index(columnList = "agent_id, delivered_at", name = "idx_history_agent_delivered"),
        @Index(columnList = "order_id", name = "idx_history_order_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "agent_id", nullable = false)
    private Integer agentId;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "restaurant_id")
    private Integer restaurantId;

    @Column(name = "restaurant_name", length = 150)
    private String restaurantName;

    @Column(name = "pickup_address", length = 500)
    private String pickupAddress;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "final_amount")
    private Double finalAmount;

    @Column(name = "mode_of_payment", length = 20)
    private String modeOfPayment;

    @Column(name = "order_status", length = 30)
    private String orderStatus;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @CreationTimestamp
    @Column(name = "delivered_at", updatable = false)
    private LocalDateTime deliveredAt;
}
