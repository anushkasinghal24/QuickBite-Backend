package com.quickbite.payment.notification.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * SendNotificationRequest
 * POST /api/v1/notifications/send
 *
 * Called by ALL other services (order-service, payment-service,
 * restaurant-service, delivery-service) when they need to notify a user.
 *
 * channel: APP | EMAIL | SMS | ALL
 *   ALL = sends on all 3 channels simultaneously.
 */
@Data
public class SendNotificationRequest {

    @NotNull(message = "recipientId is required")
    private Long recipientId;

    /** ORDER / PAYMENT / PROMO / DELIVERY / RESTAURANT / SYSTEM */
    @NotBlank(message = "type is required")
    private String type;

    @NotBlank(message = "title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 500)
    private String message;

    /**
     * APP / EMAIL / SMS / ALL
     * Default: APP (only stores in DB, no external dispatch)
     */
    private String channel = "APP";

    /** orderId / paymentId / restaurantId for deep-link */
    private Long relatedId;

    /** ORDER / PAYMENT / RESTAURANT */
    private String relatedType;

    /**
     * Deep-link URL for click navigation.
     * Example: /orders/123, /wallet, /restaurants/456
     */
    private String deepLinkUrl;

    /** Recipient email â€” required if channel = EMAIL or ALL */
    private String recipientEmail;

    /** Recipient phone â€” required if channel = SMS or ALL (format: +91XXXXXXXXXX) */
    private String recipientPhone;

    /**
     * When true, clients can treat the notification as an audible alert.
     * Used for high-priority events such as new incoming restaurant orders.
     */
    private Boolean audible;
}
