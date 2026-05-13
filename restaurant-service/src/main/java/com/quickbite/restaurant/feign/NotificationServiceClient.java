package com.quickbite.restaurant.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * NotificationServiceClient
 *
 * Sends restaurant/review related notifications through payment-service.
 */
@FeignClient(
        name = "payment-service",
        fallback = NotificationServiceClientFallback.class
)
public interface NotificationServiceClient {

    @PostMapping("/api/v1/notifications/send")
    void sendNotification(@RequestBody Map<String, Object> payload);
}
