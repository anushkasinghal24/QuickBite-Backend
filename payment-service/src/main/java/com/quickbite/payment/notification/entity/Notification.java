package com.quickbite.payment.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification Entity â€” PDF Section 4.9
 *
 * Fields (from PDF class diagram):
 *   notificationId, recipientId, type (ORDER/PAYMENT/PROMO/DELIVERY),
 *   title, message, channel (APP/EMAIL/SMS), relatedId, relatedType,
 *   isRead, sentAt
 *
 * PDF Section 2.7:
 *   "Each notification carries recipientId, type, relatedId (orderId),
 *    and a deep-link URL."
 *
 * Stored in quickbite_notifications DB.
 * Only APP channel notifications are persisted (EMAIL/SMS are fire-and-forget).
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_recipient_id",    columnList = "recipient_id"),
        @Index(name = "idx_is_read",         columnList = "is_read"),
        @Index(name = "idx_type",            columnList = "type"),
        @Index(name = "idx_related_id",      columnList = "related_id"),
        @Index(name = "idx_recipient_read",  columnList = "recipient_id, is_read")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    /**
     * Who receives this notification â€” userId from auth-service.
     * Can be customerId, ownerId, agentId, or adminId.
     */
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    /**
     * ORDER / PAYMENT / PROMO / DELIVERY / RESTAURANT / SYSTEM
     */
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /**
     * APP / EMAIL / SMS / ALL
     * Only APP notifications are stored in DB.
     * EMAIL and SMS are dispatched and not persisted.
     */
    @Column(name = "channel", nullable = false, length = 10)
    @Builder.Default
    private String channel = "APP";

    /**
     * The orderId, paymentId, restaurantId, etc. â€” for deep-link navigation.
     * PDF: "Each notification carries relatedId (orderId) and a deep-link URL."
     */
    @Column(name = "related_id")
    private Long relatedId;

    /** ORDER / PAYMENT / RESTAURANT */
    @Column(name = "related_type", length = 20)
    private String relatedType;

    /**
     * Deep-link URL for navigation on click.
     * Example: /orders/123, /wallet, /restaurants/456
     */
    @Column(name = "deep_link_url", length = 300)
    private String deepLinkUrl;

    /**
     * Read / Unread state â€” PDF Section 2.7:
     * "Unread badge count displayed in real time in the navigation bar."
     */
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    /**
     * UI hint for client apps to play a sound for urgent events.
     * New incoming restaurant orders use this flag.
     */
    @Column(name = "is_audible")
    @Builder.Default
    private Boolean audible = false;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
