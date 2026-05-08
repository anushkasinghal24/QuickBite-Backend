package com.quickbite.delivery_service.feign;

import com.quickbite.delivery_service.dto.ApiResponse;
import com.quickbite.delivery_service.dto.RestaurantDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * RestaurantServiceClient
 *
 * Used to fetch pickup location details for assigned orders.
 */
@FeignClient(name = "restaurant-service", fallback = RestaurantServiceClientFallback.class)
public interface RestaurantServiceClient {

    @GetMapping("/restaurants/{restaurantId}")
    ApiResponse<RestaurantDetailsDTO> getRestaurantById(@PathVariable("restaurantId") Long restaurantId);
}
