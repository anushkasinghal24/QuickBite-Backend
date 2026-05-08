package com.quickbite.delivery_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.quickbite.delivery_service.entity.DeliveryAgent;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Full agent profile response.
 * Returned after register, get by ID, verify, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentResponse {

    private Integer agentId;
    private Integer userId;
    private String fullName;
    private String phone;
    private String vehicleType;
    private String vehicleNumber;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime locationUpdatedAt;
    private Boolean isAvailable;
    private Boolean isVerified;
    private String status;
    private Integer currentOrderId;
    private Double avgRating;
    private Integer totalDeliveries;
    private Double totalEarnings;
    private String adminRemarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Converts entity to response DTO */
    public static AgentResponse from(DeliveryAgent agent) {
        return AgentResponse.builder()
                .agentId(agent.getAgentId())
                .userId(agent.getUserId())
                .fullName(agent.getFullName())
                .phone(agent.getPhone())
                .vehicleType(agent.getVehicleType() != null ? agent.getVehicleType().name() : null)
                .vehicleNumber(agent.getVehicleNumber())
                .currentLatitude(agent.getCurrentLatitude())
                .currentLongitude(agent.getCurrentLongitude())
                .locationUpdatedAt(agent.getLocationUpdatedAt())
                .isAvailable(agent.getIsAvailable())
                .isVerified(agent.getIsVerified())
                .status(agent.getStatus() != null ? agent.getStatus().name() : null)
                .currentOrderId(agent.getCurrentOrderId())
                .avgRating(agent.getAvgRating())
                .totalDeliveries(agent.getTotalDeliveries())
                .totalEarnings(agent.getTotalEarnings())
                .adminRemarks(agent.getAdminRemarks())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }
}
