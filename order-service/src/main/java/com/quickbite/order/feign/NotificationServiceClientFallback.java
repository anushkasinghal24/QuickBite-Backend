package com.quickbite.order.feign;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback for NotificationServiceClient.
 *
 * IMPORTANT: Notification failure must NEVER break the main order flow.
 * As per PDF Section 6 (Availability):
 *  "graceful degradation on notification or review service outage"
 *
 * So this fallback logs the failure silently and returns null â€”
 * the caller (OrderServiceImpl) checks for null and logs a warning.
 */
@Component
@Slf4j
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public ApiResponse<Void> sendNotification(NotificationRequest request) {
        log.warn("notification-service is DOWN â€” notification NOT sent to recipient {}. " +
                 "Type: {}, RelatedId: {}", request.getRecipientId(),
                 request.getType(), request.getRelatedId());
        return null;  // caller ignores null â€” notification is best-effort
    }
}
