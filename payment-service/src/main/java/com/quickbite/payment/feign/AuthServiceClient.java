package com.quickbite.payment.feign;

import com.quickbite.payment.dto.ApiResponse;
import com.quickbite.payment.dto.UserContactDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", contextId = "paymentAuthServiceClient")
public interface AuthServiceClient {
    @GetMapping("/api/v1/auth/users/{id}")
    ApiResponse<UserContactDTO> getUserById(@PathVariable("id") Integer id);
}
