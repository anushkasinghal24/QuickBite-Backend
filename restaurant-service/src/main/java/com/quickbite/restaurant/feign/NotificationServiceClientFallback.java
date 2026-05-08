package com.quickbite.restaurant.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fallback for NotificationServiceClient.
 * Notifications are non-critical, so main restaurant flow should continue.
 */
@Component
@Slf4j
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public void sendNotification(Map<String, Object> payload) {
        log.warn("notification-service unavailable. Notification skipped: {}", payload);
    }
}
