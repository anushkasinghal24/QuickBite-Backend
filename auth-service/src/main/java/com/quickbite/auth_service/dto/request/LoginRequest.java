package com.quickbite.auth_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
