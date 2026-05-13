package com.quickbite.delivery_service.feign;

import com.quickbite.delivery_service.dto.ApiResponse;
import com.quickbite.delivery_service.dto.OrderDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * OrderServiceClient
 *
 * Feign client to call order-service when it is built.
 *
 * Currently used for:
 *  - Validating orderId before assignment
 *  - Updating order status to PICKED_UP / DELIVERED
 *
 * PDF Section 4.5 (Order-Service):
 *  updateStatus(orderId, status) — order-service exposes PUT /api/v1/orders/{id}/status
 *
 * CHANGES WHEN ORDER-SERVICE IS BUILT:
 *  1. Uncomment @FeignClient annotation
 *  2. Remove @Slf4j fallback stub
 *  3. Map response DTO to OrderResponse object
 *
 * Currently uses fallback stub so delivery-service runs without order-service.
 */
@FeignClient(
        name = "order-service",
        fallback = OrderServiceClientFallback.class
)
public interface OrderServiceClient {

    /**
     * Get order details to validate before assignment.
     * PDF Section 4.5: getOrderById()
     */
    @GetMapping("/orders/{orderId}")
    ApiResponse<OrderDetailsDTO> getOrderById(@PathVariable Integer orderId);

    /**
     * Update order status: PICKED_UP or DELIVERED.
     * PDF Section 4.5: updateStatus(orderId, status)
     * Called by delivery-service when agent picks up or completes delivery.
     */
    @PutMapping("/orders/{orderId}/status")
    void updateOrderStatus(
            @PathVariable Integer orderId,
            @RequestBody Map<String, String> statusRequest
    );
}
