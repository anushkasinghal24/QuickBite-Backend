package com.quickbite.order.dto;

import lombok.Data;

/**
 * Minimal user contact projection from auth-service.
 * Used to fan out order notifications to APP + EMAIL + SMS.
 */
@Data
public class UserContactDTO {
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
}
