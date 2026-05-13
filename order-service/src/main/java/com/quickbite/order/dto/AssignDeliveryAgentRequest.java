package com.quickbite.order.dto;

import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * AssignDeliveryAgentRequest
 *
 * Sent by Admin or System to assign a delivery agent to an order.
 * In production this is automated by the Schedule/Assignment logic
 * in DeliveryService, but the manual Admin endpoint also uses this.
 *
 * As per PDF Section 4.5: assignDeliveryAgent(int orderId, int agentId)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignDeliveryAgentRequest {

    @Positive(message = "Agent ID must be positive")
    private int agentId;
}
