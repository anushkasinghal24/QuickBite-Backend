package com.quickbite.restaurant.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** PUT /api/v1/restaurants/{id}/approve — ADMIN only */
@Data
public class ApprovalRequest {

    @NotBlank(message = "Status must be APPROVED or REJECTED")
    private String status;    // APPROVED | REJECTED

    private String rejectionReason;  // required if REJECTED
}
