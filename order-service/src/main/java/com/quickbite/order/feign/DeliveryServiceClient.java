package com.quickbite.order.feign;

import com.quickbite.order.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * DeliveryServiceClient
 *
 * Feign client for delivery-service.
 * Order-service calls delivery-service to:
 *  1. Assign an available nearby agent when order is CONFIRMED
 *  2. Mark delivery complete when agent marks DELIVERED
 *
 * As per PDF Section 4.5:
 *  "On placement, it calls... the Schedule-Service to assign a delivery agent."
 * As per PDF Section 4.7:
 *  "assignOrder(int orderId, int agentId)"
 *  "completeDelivery(int agentId)"
 */
@FeignClient(name = "delivery-service", fallback = DeliveryServiceClientFallback.class)
public interface DeliveryServiceClient {

    /**
     * Assign a delivery agent to an order.
     * delivery-service will mark the agent as unavailable until delivery is done.
     */
    @PostMapping("/agents/{agentId}/assign/{orderId}")
    ApiResponse<Void> assignOrderToAgent(
            @PathVariable("agentId") int agentId,
            @PathVariable("orderId") int orderId);

    /**
     * Mark the delivery as complete.
     * delivery-service will free up the agent slot for the next order.
     */
    @PutMapping("/agents/{agentId}/complete/{orderId}")
    ApiResponse<Void> completeDelivery(
            @PathVariable("agentId") int agentId,
            @PathVariable("orderId") int orderId);
}
