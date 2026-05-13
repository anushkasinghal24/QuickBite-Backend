package com.quickbite.delivery_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * DeliveryAgent Entity
 *
 * PDF Section 4.7 exact fields:
 *  agentId, userId, fullName, phone, vehicleType, vehicleNumber,
 *  currentLatitude, currentLongitude, isAvailable, isVerified,
 *  avgRating, totalDeliveries
 *
 * Additional fields added for completeness:
 *  - vehicleType: BIKE / CYCLE / SCOOTER / CAR
 *  - status: PENDING / VERIFIED / SUSPENDED (admin workflow)
 *  - currentOrderId: which order is currently being delivered
 *  - totalEarnings: cumulative earnings
 */
@Entity
@Table(
    name = "delivery_agents",
    indexes = {
        @Index(columnList = "user_id",     name = "idx_agent_user_id"),
        @Index(columnList = "is_available", name = "idx_agent_available"),
        @Index(columnList = "is_verified",  name = "idx_agent_verified"),
        @Index(columnList = "current_latitude, current_longitude", name = "idx_agent_location")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DeliveryAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agent_id")
    private Integer agentId;

    /**
     * FK to auth-service User (role = AGENT).
     * Cross-service reference — no JPA FK constraint.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    /** Snapshot from auth-service User.fullName */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    /** BIKE | CYCLE | SCOOTER | CAR */
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "vehicle_number", nullable = false, length = 20)
    private String vehicleNumber;

    /**
     * Live GPS — updated at configurable intervals (PDF Section 2.4)
     * Used by order-service for Haversine-based proximity assignment.
     * Also pushed to customer's tracking screen via WebSocket.
     */
    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    /** Last time location was updated */
    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    /**
     * Online/Offline toggle (PDF Section 2.4).
     * Agent must be available=true AND verified=true to receive orders.
     */
    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = false;

    /**
     * Admin verification status (PDF Section 2.4 + 2.5).
     * Must be VERIFIED before eligible for order assignments.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AgentStatus status = AgentStatus.PENDING;

    /**
     * Backward-compat flag — true when status = VERIFIED
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Currently active order (null when free).
     * Cross-service reference to order-service Order.
     */
    @Column(name = "current_order_id")
    private Integer currentOrderId;

    /**
     * Average delivery rating (1.0 - 5.0).
     * Updated by review-service after each delivery rating submission.
     * (PDF Section 4.7: avgRating, PDF Section 4.8: avgDeliveryRatingByAgentId)
     */
    @Column(name = "avg_rating")
    @Builder.Default
    private Double avgRating = 0.0;

    /** Total completed deliveries */
    @Column(name = "total_deliveries")
    @Builder.Default
    private Integer totalDeliveries = 0;

    /** Cumulative earnings in ₹ */
    @Column(name = "total_earnings")
    @Builder.Default
    private Double totalEarnings = 0.0;

    /** Admin rejection/suspension remarks */
    @Column(name = "admin_remarks", length = 500)
    private String adminRemarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Enums ────────────────────────────────────────────────────────────────

    public enum VehicleType {
        BIKE, CYCLE, SCOOTER, CAR
    }

    public enum AgentStatus {
        PENDING,    // Just registered, awaiting admin verification
        VERIFIED,   // Admin verified — eligible for orders
        SUSPENDED,  // Admin suspended
        REJECTED    // Admin rejected (documents incomplete)
    }

    // ─── Convenience ─────────────────────────────────────────────────────────

    /** Agent is eligible to receive order assignments */
    public boolean isEligibleForOrders() {
        return Boolean.TRUE.equals(isVerified)
                && Boolean.TRUE.equals(isAvailable)
                && status == AgentStatus.VERIFIED
                && currentOrderId == null;
    }
}
