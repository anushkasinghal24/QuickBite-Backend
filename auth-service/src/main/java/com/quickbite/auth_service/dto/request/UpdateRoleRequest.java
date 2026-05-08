package com.quickbite.auth_service.dto.request;

import com.quickbite.auth_service.entity.User.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;
}
