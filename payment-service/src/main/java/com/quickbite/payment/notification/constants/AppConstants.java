package com.quickbite.payment.notification.constants;

/**
 * AppConstants â€” all notification types, channels, and messages.
 *
 * PDF Section 2.7:
 *   Notifications for: order placed, confirmed, preparing, picked up, delivered.
 *   Each carries: recipientId, type, relatedId (orderId), deep-link URL.
 *   Multi-channel: APP / EMAIL / SMS
 */
public class AppConstants {

    private AppConstants() {}

    // ===== NOTIFICATION TYPES (PDF Section 4.9) =====
    public static final String TYPE_ORDER           = "ORDER";
    public static final String TYPE_PAYMENT         = "PAYMENT";
    public static final String TYPE_PROMO           = "PROMO";
    public static final String TYPE_DELIVERY        = "DELIVERY";
    public static final String TYPE_RESTAURANT      = "RESTAURANT";
    public static final String TYPE_SYSTEM          = "SYSTEM";

    // ===== ORDER LIFECYCLE SUBTYPES (PDF Section 2.7) =====
    public static final String ORDER_PLACED         = "ORDER_PLACED";
    public static final String ORDER_CONFIRMED      = "ORDER_CONFIRMED";
    public static final String ORDER_PREPARING      = "ORDER_PREPARING";
    public static final String ORDER_PICKED_UP      = "ORDER_PICKED_UP";
    public static final String ORDER_DELIVERED      = "ORDER_DELIVERED";
    public static final String ORDER_CANCELLED      = "ORDER_CANCELLED";

    // ===== PAYMENT TYPES =====
    public static final String PAYMENT_RECEIPT      = "PAYMENT_RECEIPT";
    public static final String REFUND_INITIATED     = "REFUND_INITIATED";
    public static final String WALLET_TOPUP         = "WALLET_TOPUP";

    // ===== RESTAURANT TYPES =====
    public static final String RESTAURANT_APPROVED  = "RESTAURANT_APPROVED";
    public static final String RESTAURANT_REJECTED  = "RESTAURANT_REJECTED";
    public static final String NEW_ORDER_ALERT      = "NEW_ORDER_ALERT";  // audio + in-app for owner

    // ===== AGENT TYPES =====
    public static final String AGENT_VERIFIED       = "AGENT_VERIFIED";
    public static final String AGENT_ASSIGNED       = "AGENT_ASSIGNED";

    // ===== CHANNELS (PDF Section 4.9: APP / EMAIL / SMS) =====
    public static final String CHANNEL_APP          = "APP";
    public static final String CHANNEL_EMAIL        = "EMAIL";
    public static final String CHANNEL_SMS          = "SMS";
    public static final String CHANNEL_ALL          = "ALL";   // sends on all 3 channels

    // ===== RELATED TYPES =====
    public static final String RELATED_ORDER        = "ORDER";
    public static final String RELATED_PAYMENT      = "PAYMENT";
    public static final String RELATED_RESTAURANT   = "RESTAURANT";

    // ===== ROLES =====
    public static final String ROLE_CUSTOMER        = "CUSTOMER";
    public static final String ROLE_OWNER           = "OWNER";
    public static final String ROLE_AGENT           = "AGENT";
    public static final String ROLE_ADMIN           = "ADMIN";

    // ===== PAGINATION =====
    public static final int    DEFAULT_PAGE         = 0;
    public static final int    DEFAULT_PAGE_SIZE    = 20;

    // ===== NOTIFICATION MESSAGES =====
    public static final String MSG_ORDER_PLACED     = "Your order #%d has been placed successfully!";
    public static final String MSG_ORDER_CONFIRMED  = "Your order #%d has been confirmed by the restaurant.";
    public static final String MSG_ORDER_PREPARING  = "The restaurant is preparing your order #%d.";
    public static final String MSG_ORDER_PICKED_UP  = "Your order #%d has been picked up and is on the way!";
    public static final String MSG_ORDER_DELIVERED  = "Your order #%d has been delivered. Enjoy your meal!";
    public static final String MSG_ORDER_CANCELLED  = "Your order #%d has been cancelled.";
    public static final String MSG_NEW_ORDER        = "New order #%d received! Check your dashboard.";
    public static final String MSG_PAYMENT_RECEIPT  = "Payment of â‚¹%.2f received for order #%d. TXN: %s";
    public static final String MSG_REFUND           = "Refund of â‚¹%.2f initiated for order #%d.";
    public static final String MSG_WALLET_TOPUP     = "â‚¹%.2f added to your wallet. New balance: â‚¹%.2f";
}
