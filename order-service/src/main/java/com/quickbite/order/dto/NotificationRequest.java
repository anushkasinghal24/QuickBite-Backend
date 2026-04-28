package com.quickbite.order.dto;

import lombok.*;

/**
 * NotificationRequest
 *
 * Sent to notification-service via Feign when order status changes.
 * As per PDF Section 2.7:
 *  "Each notification carries recipientId, type, relatedId (orderId), and a deep-link URL"
 *
 * Notification types triggered by order-service:
 *  - ORDER_PLACED       â†’ customer
 *  - ORDER_CONFIRMED    â†’ customer
 *  - ORDER_PREPARING    â†’ customer
 *  - ORDER_PICKED_UP    â†’ customer
 *  - ORDER_DELIVERED    â†’ customer
 *  - NEW_ORDER          â†’ restaurant owner (new incoming order ping)
 *  - ORDER_CANCELLED    â†’ customer + restaurant
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    /** ID of the user who should receive this notification */
    private int recipientId;

    /**
     * Notification type.
     * Maps to Notification.type in notification-service.
     * ORDER | PAYMENT | PROMO | DELIVERY
     */
    private String type;

    /** Human-readable notification title */
    private String title;

    /** Notification body message */
    private String message;

    /** The orderId this notification relates to */
    private int relatedId;

    /** Deep link URL for the notification (e.g. /orders/123/track) */
    private String deepLinkUrl;

    /** Notification channel: APP | EMAIL | SMS */
    private String channel;

    /** True when the client should play an audio cue for this alert. */
    private Boolean audible;
}
