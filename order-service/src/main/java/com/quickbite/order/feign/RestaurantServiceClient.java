package com.quickbite.order.feign;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.RestaurantDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * RestaurantServiceClient
 *
 * Feign client for restaurant-service.
 * Order-service calls restaurant-service to:
 *  1. Validate restaurant is open and approved before placing order
 *  2. Check minimum order amount threshold
 *  3. Snapshot restaurant name into Order entity
 *  4. Get estimated delivery time
 */
@FeignClient(
        name = "restaurant-service",
        contextId = "orderRestaurantServiceClient",
        fallback = RestaurantServiceClientFallback.class)
public interface RestaurantServiceClient {

    /**
     * Fetch restaurant by ID to validate open/approved status.
     * Endpoint matches RestaurantResource in restaurant-service.
     */
    @GetMapping("/api/v1/restaurants/{restaurantId}")
    ApiResponse<RestaurantDTO> getRestaurantById(@PathVariable("restaurantId") int restaurantId);
}
