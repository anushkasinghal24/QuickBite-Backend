package com.quickbite.delivery_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * NotificationServiceClient
 *
 * EXACT same pattern as restaurant-service/feign/NotificationClient.java
 *
 * Sends notifications when:
 *  - Agent registered           → notify admin (new verification pending)
 *  - Agent verified             → notify agent (account active)
 *  - Agent rejected/suspended   → notify agent (reason given)
 *  - Order assigned             → notify agent (new pickup)
 *  - Delivery complete          → notify agent (earnings credited)
 *
 * Eureka discovers notification-service automatically via lb://notification-service.
 * Fallback: NotificationServiceClientFallback (PDF NFR: graceful degradation).
 *
 * When notification-service is built:
 *  - It must expose: POST /api/v1/notifications/send
 *  - Payload: { type, recipientId, title, message, relatedId, relatedType }
 *  - No changes needed in delivery-service Feign client.
 */
@FeignClient(
        name = "notification-service",
        fallback = NotificationServiceClientFallback.class
)
public interface NotificationServiceClient {

    @PostMapping("/api/v1/notifications/send")
    void sendNotification(
            @RequestBody Map<String, Object> payload);
}
