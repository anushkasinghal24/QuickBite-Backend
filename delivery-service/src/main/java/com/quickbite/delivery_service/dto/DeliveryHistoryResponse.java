package com.quickbite.delivery_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.quickbite.delivery_service.entity.DeliveryHistory;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryHistoryResponse {

    private Long historyId;
    private Integer agentId;
    private Integer orderId;
    private Integer customerId;
    private String customerName;
    private Integer restaurantId;
    private String restaurantName;
    private String pickupAddress;
    private String deliveryAddress;
    private Double finalAmount;
    private String modeOfPayment;
    private String orderStatus;
    private Integer itemCount;
    private LocalDateTime orderDate;
    private LocalDateTime deliveredAt;

    public static DeliveryHistoryResponse from(DeliveryHistory history) {
        return DeliveryHistoryResponse.builder()
                .historyId(history.getHistoryId())
                .agentId(history.getAgentId())
                .orderId(history.getOrderId())
                .customerId(history.getCustomerId())
                .customerName(history.getCustomerName())
                .restaurantId(history.getRestaurantId())
                .restaurantName(history.getRestaurantName())
                .pickupAddress(history.getPickupAddress())
                .deliveryAddress(history.getDeliveryAddress())
                .finalAmount(history.getFinalAmount())
                .modeOfPayment(history.getModeOfPayment())
                .orderStatus(history.getOrderStatus())
                .itemCount(history.getItemCount())
                .orderDate(history.getOrderDate())
                .deliveredAt(history.getDeliveredAt())
                .build();
    }
}
