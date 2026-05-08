package com.quickbite.delivery_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ADMIN: Verify, reject, or suspend a delivery agent.
 * PDF Section 2.5: "Verify delivery agent identity and vehicle documents before activating."
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyAgentRequest {

    /**
     * Action to perform:
     *   VERIFY   → set status=VERIFIED, isVerified=true
     *   REJECT   → set status=REJECTED
     *   SUSPEND  → set status=SUSPENDED, isAvailable=false
     */
    @NotBlank(message = "Action is required: VERIFY | REJECT | SUSPEND")
    @Pattern(regexp = "^(VERIFY|REJECT|SUSPEND)$", message = "Action must be VERIFY, REJECT, or SUSPEND")
    private String action;

    /** Reason for rejection or suspension (optional for VERIFY, required for others) */
    @Size(max = 500, message = "Remarks max 500 chars")
    private String remarks;
}
