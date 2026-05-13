package com.quickbite.order.feign;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.AssignOrderRequest;
import com.quickbite.order.dto.NearbyAgentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fallback for DeliveryServiceClient.
 */
@Component
@Slf4j
public class DeliveryServiceClientFallback implements DeliveryServiceClient {

    @Override
    public ApiResponse<Void> assignOrderToAgent(int agentId, AssignOrderRequest request) {
        log.error("delivery-service is DOWN - could not assign agent {} to order {}",
                agentId, request != null ? request.getOrderId() : null);
        return null;
    }

    @Override
    public ApiResponse<List<NearbyAgentDTO>> getNearbyAgents(Double lat, Double lng, Double radius) {
        log.error("delivery-service is DOWN - could not fetch nearby agents");
        return null;
    }

    @Override
    public ApiResponse<Void> completeDelivery(int agentId, int orderId) {
        log.error("delivery-service is DOWN - could not complete delivery for agent {} order {}",
                agentId, orderId);
        return null;
    }
}
