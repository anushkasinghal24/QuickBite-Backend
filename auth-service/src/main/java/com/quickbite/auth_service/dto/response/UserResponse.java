package com.quickbite.auth_service.dto.response;

import com.quickbite.auth_service.entity.User;
import com.quickbite.auth_service.entity.User.Role;
import com.quickbite.auth_service.entity.User.AuthProvider;
import lombok.*;
import java.time.LocalDateTime;

/**
 * UserResponse — returned by GET /api/v1/auth/users/{id}
 *
 * Used by:
 *  - restaurant-service: GET ownerId details
 *  - delivery-service: GET agent user details
 *  - notification-service: GET email for sending mails
 *  - Admin dashboard: user management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private AuthProvider provider;
    private Boolean isActive;
    private String profilePicUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .provider(user.getProvider())
                .isActive(user.getIsActive())
                .profilePicUrl(user.getProfilePicUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
