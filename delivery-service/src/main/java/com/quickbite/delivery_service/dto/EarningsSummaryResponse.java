package com.quickbite.delivery_service.dto;

import com.quickbite.delivery_service.entity.DeliveryAgent;
import lombok.*;

/**
 * Earnings summary for delivery agent dashboard.
 * PDF Section 2.4: "View earnings summary and customer delivery ratings."
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EarningsSummaryResponse {

    private Integer agentId;
    private String fullName;
    private Integer totalDeliveries;
    private Double totalEarnings;
    private Double avgRating;
    private Boolean isAvailable;
    private String status;

    public static EarningsSummaryResponse from(DeliveryAgent agent) {
        return EarningsSummaryResponse.builder()
                .agentId(agent.getAgentId())
                .fullName(agent.getFullName())
                .totalDeliveries(agent.getTotalDeliveries())
                .totalEarnings(agent.getTotalEarnings())
                .avgRating(agent.getAvgRating())
                .isAvailable(agent.getIsAvailable())
                .status(agent.getStatus() != null ? agent.getStatus().name() : null)
                .build();
    }
}
