package com.quickbite.restaurant.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fallback for DeliveryServiceClient.
 * Rating propagation is non-critical and should not break review flows.
 */
@Component
@Slf4j
public class DeliveryServiceClientFallback implements DeliveryServiceClient {

    @Override
    public void updateAgentRating(Integer agentId, Map<String, Object> payload) {
        log.warn("delivery-service unavailable. Agent rating update skipped for agentId={}, payload={}",
                agentId, payload);
    }
}
