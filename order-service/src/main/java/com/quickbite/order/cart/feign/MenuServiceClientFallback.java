package com.quickbite.order.cart.feign;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.cart.dto.MenuItemDTO;
import org.springframework.stereotype.Component;

/**
 * Fallback for MenuServiceClient.
 * When menu-service is unavailable (e.g., during development or outage),
 * this fallback prevents cart-service from crashing.
 *
 * NOTE: Until menu-service is built, cart-service will use this fallback.
 * Once menu-service is ready, real Feign calls will work automatically.
 */
@Component
public class MenuServiceClientFallback implements MenuServiceClient {

    @Override
    public ApiResponse<MenuItemDTO> getMenuItemById(int itemId) {
        // Return null to signal menu-service is unavailable
        // CartServiceImpl handles this case and throws appropriate exception
        return null;
    }
}
