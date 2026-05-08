package com.quickbite.delivery_service.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fallback for NotificationServiceClient.
 *
 * PDF NFR Section 6: "graceful degradation on notification service outage"
 *
 * If notification-service is down:
 *  - Delivery operations continue normally
 *  - Notifications are simply dropped (logged as WARN)
 *  - No exception propagated to caller
 *
 * In production: implement retry queue / RabbitMQ dead-letter queue
 * (PDF mentions RabbitMQ for async order-event notification dispatch).
 */
@Component
@Slf4j
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public void sendNotification(Map<String, Object> payload) {
        log.warn("notification-service unavailable. Notification dropped: type={}, recipientId={}",
                payload.get("type"), payload.get("recipientId"));
        // Non-blocking — delivery-service continues normally
    }
}
