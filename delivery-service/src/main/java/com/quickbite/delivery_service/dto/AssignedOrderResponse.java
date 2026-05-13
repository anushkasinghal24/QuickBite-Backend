package com.quickbite.delivery_service.dto;

import com.quickbite.delivery_service.entity.DeliveryAgent;
import lombok.*;

import java.time.LocalDateTime;

/**
 * AssignedOrderResponse
 *
 * Delivery agent view of the currently assigned order.
 * Includes pickup and delivery location data for the driver app.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignedOrderResponse {

    private Integer orderId;
    private Integer agentId;
    private Integer restaurantId;
    private String restaurantName;
    private String pickupAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String customerName;
    private String deliveryAddress;
    private String orderStatus;
    private LocalDateTime orderDate;
    private LocalDateTime estimatedDelivery;

    public static AssignedOrderResponse from(Integer orderId,
                                             DeliveryAgent agent,
                                             OrderDetailsDTO order,
                                             RestaurantDetailsDTO restaurant) {
        return AssignedOrderResponse.builder()
                .orderId(orderId)
                .agentId(agent.getAgentId())
                .restaurantId(order.getRestaurantId())
                .restaurantName(order.getRestaurantName())
                .pickupAddress(restaurant.getAddress())
                .pickupLatitude(restaurant.getLatitude())
                .pickupLongitude(restaurant.getLongitude())
                .customerName(order.getCustomerName())
                .deliveryAddress(order.getDeliveryAddress())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .estimatedDelivery(order.getEstimatedDelivery())
                .build();
    }
}
