package com.quickbite.auth_service.dto.response;

import com.quickbite.auth_service.entity.User.Role;
import com.quickbite.auth_service.entity.User.AuthProvider;
import lombok.*;

import java.time.LocalDateTime;

// ═══════════════════════════════════════════════════════════════════════════════
// RESPONSE DTOs — All outbound payloads from Auth Service APIs
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * AuthResponse — returned on login / register / refresh
 *
 * Other services (restaurant-service, order-service) will receive this
 * in their Feign client responses when they validate tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private String refreshToken;

    /** Access token expiry in milliseconds */
    private Long expiresIn;

    /** User basic info embedded so frontend doesn't need a second call */
    private UserSummary user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Integer userId;
        private String fullName;
        private String email;
        private String phone;
        private Role role;
        private AuthProvider provider;
        private Boolean isActive;
        private String profilePicUrl;
        private LocalDateTime createdAt;
    }
}
