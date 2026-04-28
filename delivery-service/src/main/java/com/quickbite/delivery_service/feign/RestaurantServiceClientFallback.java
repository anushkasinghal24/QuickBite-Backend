package com.quickbite.delivery_service.feign;

import com.quickbite.delivery_service.dto.ApiResponse;
import com.quickbite.delivery_service.dto.RestaurantDetailsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback for RestaurantServiceClient.
 */
@Component
@Slf4j
public class RestaurantServiceClientFallback implements RestaurantServiceClient {

    @Override
    public ApiResponse<RestaurantDetailsDTO> getRestaurantById(Long restaurantId) {
        log.warn("restaurant-service unavailable. Returning stub for restaurantId={}", restaurantId);
        RestaurantDetailsDTO stub = RestaurantDetailsDTO.builder()
                .restaurantId(restaurantId)
                .name("Restaurant #" + restaurantId)
                .address("Pickup address unavailable")
                .isOpen(true)
                .approvalStatus("APPROVED")
                .build();
        return ApiResponse.success("Stub restaurant response", stub);
    }
}
