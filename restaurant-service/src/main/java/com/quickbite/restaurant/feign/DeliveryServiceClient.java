package com.quickbite.restaurant.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * DeliveryServiceClient
 *
 * Pushes delivery-agent rating updates to delivery-service.
 */
@FeignClient(
        name = "delivery-service",
        fallback = DeliveryServiceClientFallback.class
)
public interface DeliveryServiceClient {

    @PutMapping("/api/v1/agents/{agentId}/rating")
    void updateAgentRating(@PathVariable("agentId") Integer agentId,
                           @RequestBody Map<String, Object> payload);
}
