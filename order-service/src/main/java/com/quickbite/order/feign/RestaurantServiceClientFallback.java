package com.quickbite.order.feign;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.RestaurantDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback for RestaurantServiceClient.
 * If restaurant-service is down, order placement will fail gracefully.
 */
@Component
@Slf4j
public class RestaurantServiceClientFallback implements RestaurantServiceClient {

    @Override
    public ApiResponse<RestaurantDTO> getRestaurantById(int restaurantId) {
        log.error("restaurant-service is DOWN â€” cannot validate restaurant {}", restaurantId);
        return null;
    }
}
