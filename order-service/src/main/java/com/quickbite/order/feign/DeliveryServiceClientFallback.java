package com.quickbite.order.feign;

import com.quickbite.order.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback for DeliveryServiceClient.
 * Agent assignment failure is logged; order still proceeds
 * (admin can manually assign later via PUT /orders/{id}/agent).
 */
@Component
@Slf4j
public class DeliveryServiceClientFallback implements DeliveryServiceClient {

    @Override
    public ApiResponse<Void> assignOrderToAgent(int agentId, int orderId) {
        log.error("delivery-service is DOWN â€” could not assign agent {} to order {}",
                agentId, orderId);
        return null;
    }

    @Override
    public ApiResponse<Void> completeDelivery(int agentId, int orderId) {
        log.error("delivery-service is DOWN â€” could not complete delivery for agent {} order {}",
                agentId, orderId);
        return null;
    }
}
