package com.quickbite.order.dto;

import lombok.*;

/**
 * Minimal nearby delivery agent payload used for pickup assignment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyAgentDTO {
    private Integer agentId;
    private Integer userId;
    private String fullName;
    private Double currentLatitude;
    private Double currentLongitude;
    private Boolean isAvailable;
    private Boolean isVerified;
    private String status;
    private Integer currentOrderId;
}
