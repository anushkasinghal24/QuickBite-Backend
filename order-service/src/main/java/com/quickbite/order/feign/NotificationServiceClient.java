package com.quickbite.order.feign;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * NotificationServiceClient
 *
 * Feign client for payment-service notification endpoints.
 * Order-service calls this after every order lifecycle event to dispatch
 * multi-channel alerts (in-app, email, SMS).
 *
 * As per PDF Section 2.7:
 *  "Notifications dispatched for: order placed, order confirmed by restaurant,
 *   food being prepared, picked up by agent, delivered."
 *  "Restaurant owners receive audio + in-app alert for every new incoming order."
 *
 * Notification calls are NON-BLOCKING â€” failures are logged but never
 * prevent the main order operation from completing.
 */
@FeignClient(name = "payment-service", fallback = NotificationServiceClientFallback.class)
public interface NotificationServiceClient {

    /**
     * Send a single notification to a recipient.
     * Endpoint must match NotificationResource in notification-service.
     */
    @PostMapping("/api/v1/notifications/send")
    ApiResponse<Void> sendNotification(@RequestBody NotificationRequest request);
}
