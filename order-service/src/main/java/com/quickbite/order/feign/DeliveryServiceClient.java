package com.quickbite.order.feign;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.AssignOrderRequest;
import com.quickbite.order.dto.NearbyAgentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * DeliveryServiceClient
 *
 * Feign client for delivery-service.
 */
@FeignClient(name = "delivery-service", fallback = DeliveryServiceClientFallback.class)
public interface DeliveryServiceClient {

    @PostMapping("/api/v1/agents/{agentId}/assign-order")
    ApiResponse<Void> assignOrderToAgent(
            @PathVariable("agentId") int agentId,
            @RequestBody AssignOrderRequest request);

    @GetMapping("/api/v1/agents/nearby")
    ApiResponse<List<NearbyAgentDTO>> getNearbyAgents(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam(value = "radius", required = false) Double radius);

    @PutMapping("/api/v1/agents/{agentId}/complete/{orderId}")
    ApiResponse<Void> completeDelivery(
            @PathVariable("agentId") int agentId,
            @PathVariable("orderId") int orderId);
}
