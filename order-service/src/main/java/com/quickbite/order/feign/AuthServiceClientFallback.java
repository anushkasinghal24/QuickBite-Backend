package com.quickbite.order.feign;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.UserContactDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Best-effort fallback. Order placement must not fail if auth-service is unavailable.
 */
@Component
@Slf4j
public class AuthServiceClientFallback implements AuthServiceClient {

    @Override
    public ApiResponse<UserContactDTO> getUserById(Integer id) {
        log.warn("auth-service unavailable while resolving contact details for user {}", id);
        return null;
    }
}
