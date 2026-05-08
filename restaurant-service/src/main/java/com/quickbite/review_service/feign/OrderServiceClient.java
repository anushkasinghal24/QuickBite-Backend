package com.quickbite.review_service.feign;

import com.quickbite.review_service.dto.response.ApiResponse;
import com.quickbite.review_service.dto.response.OrderDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OrderServiceClient
 *
 * Used by review-service to validate that:
 * - the order exists
 * - the order belongs to the signed-in customer
 * - the order has been delivered before review submission
 */
@FeignClient(
        name = "order-service",
        fallback = OrderServiceClientFallback.class
)
public interface OrderServiceClient {

    @GetMapping("/orders/{orderId}")
    ApiResponse<OrderDetailsDTO> getOrderById(@PathVariable Integer orderId);
}
