package com.quickbite.delivery_service.feign;

import com.quickbite.delivery_service.dto.ApiResponse;
import com.quickbite.delivery_service.dto.OrderDetailsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fallback for OrderServiceClient.
 * Allows delivery-service to run standalone before order-service is built.
 */
@Component
@Slf4j
public class OrderServiceClientFallback implements OrderServiceClient {

    @Override
    public ApiResponse<OrderDetailsDTO> getOrderById(Integer orderId) {
        log.warn("order-service unavailable. Returning stub for orderId={}", orderId);
        OrderDetailsDTO stub = OrderDetailsDTO.builder()
                .orderId(orderId)
                .restaurantName("Unknown restaurant")
                .deliveryAddress("Unavailable")
                .orderStatus("UNKNOWN")
                .build();
        return ApiResponse.success("Stub order response", stub);
    }

    @Override
    public void updateOrderStatus(Integer orderId, Map<String, String> statusRequest) {
        log.warn("order-service unavailable. Order status update skipped for orderId={}", orderId);
    }
}
