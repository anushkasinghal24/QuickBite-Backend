package com.quickbite.review_service.feign;

import com.quickbite.review_service.dto.response.ApiResponse;
import com.quickbite.review_service.dto.response.OrderDetailsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Safe fallback when order-service is unavailable.
 */
@Component
public class OrderServiceClientFallback implements OrderServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceClientFallback.class);

    @Override
    public ApiResponse<OrderDetailsDTO> getOrderById(Integer orderId) {
        log.warn("order-service unavailable while validating review for orderId={}", orderId);
        return ApiResponse.error("Order service unavailable");
    }
}
