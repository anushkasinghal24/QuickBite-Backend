package com.quickbite.auth_service.dto.response;

import com.quickbite.auth_service.entity.User.Role;
import lombok.*;

/**
 * TokenValidationResponse
 *
 * Returned by POST /api/v1/auth/validate-token
 *
 * CRITICAL: This endpoint is called by:
 *  1. API Gateway — to validate every incoming request before routing
 *  2. Other microservices (restaurant, order, cart, payment, delivery, review)
 *     when they need to know WHO is making the request and WHAT ROLE they have.
 *
 * Without this, each service would need its own JWT secret — bad practice.
 * Centralized token validation = single source of truth.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {

    private Boolean valid;

    /** User ID — other services use this as foreign key */
    private Integer userId;

    private String email;

    private String fullName;

    /** Role — used for authorization decisions in other services */
    private Role role;

    private Boolean isActive;

    private String message;  // Error message if invalid
}
