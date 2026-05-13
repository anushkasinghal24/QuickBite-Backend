package com.quickbite.payment.notification.constants;

/**
 * AppConstants - notification types, channels, and messages.
 */
public class AppConstants {

    private AppConstants() {}

    public static final String TYPE_ORDER = "ORDER";
    public static final String TYPE_PAYMENT = "PAYMENT";
    public static final String TYPE_PROMO = "PROMO";
    public static final String TYPE_DELIVERY = "DELIVERY";
    public static final String TYPE_RESTAURANT = "RESTAURANT";
    public static final String TYPE_SYSTEM = "SYSTEM";

    public static final String ORDER_PLACED = "ORDER_PLACED";
    public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String ORDER_PREPARING = "ORDER_PREPARING";
    public static final String ORDER_PICKED_UP = "ORDER_PICKED_UP";
    public static final String ORDER_DELIVERED = "ORDER_DELIVERED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    public static final String PAYMENT_RECEIPT = "PAYMENT_RECEIPT";
    public static final String REFUND_INITIATED = "REFUND_INITIATED";
    public static final String WALLET_TOPUP = "WALLET_TOPUP";

    public static final String RESTAURANT_APPROVED = "RESTAURANT_APPROVED";
    public static final String RESTAURANT_REJECTED = "RESTAURANT_REJECTED";
    public static final String NEW_ORDER_ALERT = "NEW_ORDER_ALERT";

    public static final String AGENT_VERIFIED = "AGENT_VERIFIED";
    public static final String AGENT_ASSIGNED = "AGENT_ASSIGNED";

    public static final String CHANNEL_APP = "APP";
    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_ALL = "ALL";

    public static final String RABBIT_NOTIFICATION_EXCHANGE = "quickbite.payment.notification.exchange";
    public static final String RABBIT_NOTIFICATION_QUEUE = "quickbite.payment.notification.queue";
    public static final String RABBIT_NOTIFICATION_KEY = "quickbite.payment.notification";
    public static final String RABBIT_NOTIFICATION_EMAIL_QUEUE = "quickbite.payment.notification.email.queue";
    public static final String RABBIT_NOTIFICATION_SMS_QUEUE = "quickbite.payment.notification.sms.queue";
    public static final String RABBIT_NOTIFICATION_QUEUE_BIND = "quickbite.payment.notification";
    public static final String RABBIT_NOTIFICATION_EMAIL_KEY = "quickbite.payment.notification.email";
    public static final String RABBIT_NOTIFICATION_SMS_KEY = "quickbite.payment.notification.sms";

    public static final String RELATED_ORDER = "ORDER";
    public static final String RELATED_PAYMENT = "PAYMENT";
    public static final String RELATED_RESTAURANT = "RESTAURANT";

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_AGENT = "AGENT";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;

    public static final String MSG_ORDER_PLACED = "Your order #%d has been placed successfully!";
    public static final String MSG_ORDER_CONFIRMED = "Your order #%d has been confirmed by the restaurant.";
    public static final String MSG_ORDER_PREPARING = "The restaurant is preparing your order #%d.";
    public static final String MSG_ORDER_PICKED_UP = "Your order #%d has been picked up and is on the way!";
    public static final String MSG_ORDER_DELIVERED = "Your order #%d has been delivered. Enjoy your meal!";
    public static final String MSG_ORDER_CANCELLED = "Your order #%d has been cancelled.";
    public static final String MSG_NEW_ORDER = "New order #%d received! Check your dashboard.";
    public static final String MSG_PAYMENT_RECEIPT = "Payment of Rs. %.2f received for order #%d. TXN: %s";
    public static final String MSG_REFUND = "Refund of Rs. %.2f initiated for order #%d.";
    public static final String MSG_WALLET_TOPUP = "Rs. %.2f added to your wallet. New balance: Rs. %.2f";
}
