package com.quickbite.order.feign;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.UserContactDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * AuthServiceClient
 *
 * Used by order-service to resolve a user's email/phone for notifications.
 */
@FeignClient(
        name = "auth-service",
        contextId = "orderAuthServiceClient",
        fallback = AuthServiceClientFallback.class
)
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{id}")
    ApiResponse<UserContactDTO> getUserById(@PathVariable("id") Integer id);
}
