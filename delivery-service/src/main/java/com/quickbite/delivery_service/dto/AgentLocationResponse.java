package com.quickbite.delivery_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.quickbite.delivery_service.entity.DeliveryAgent;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Minimal agent location response for customer order tracking screen.
 * PDF Section 2.4: "Update live GPS for real-time tracking by customers."
 * PDF NFR: "WebSocket (STOMP) every 15 seconds"
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentLocationResponse {

    private Integer agentId;
    private String fullName;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime locationUpdatedAt;
    private Double avgRating;
    private String vehicleType;

    public static AgentLocationResponse from(DeliveryAgent agent) {
        return AgentLocationResponse.builder()
                .agentId(agent.getAgentId())
                .fullName(agent.getFullName())
                .currentLatitude(agent.getCurrentLatitude())
                .currentLongitude(agent.getCurrentLongitude())
                .locationUpdatedAt(agent.getLocationUpdatedAt())
                .avgRating(agent.getAvgRating())
                .vehicleType(agent.getVehicleType() != null ? agent.getVehicleType().name() : null)
                .build();
    }
}
