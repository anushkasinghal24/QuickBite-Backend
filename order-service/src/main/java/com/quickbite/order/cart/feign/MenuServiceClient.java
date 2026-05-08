package com.quickbite.order.cart.feign;

import com.quickbite.order.dto.ApiResponse;
import  com.quickbite.order.cart.dto.MenuItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client for restaurant-service menu endpoints.
 *
 * When menu-service is built, cart-service will call it to:
 *  1. Fetch item details (name, price, isVeg, imageUrl) for snapshot on addItem
 *  2. Validate item belongs to the restaurantId being added
 *  3. Check isAvailable before adding
 *
 * "name = restaurant-service" must match spring.application.name in restaurant-service.
 */
@FeignClient(
        name = "restaurant-service",
        contextId = "cartMenuServiceClient",
        fallback = com.quickbite.order.cart.feign.MenuServiceClientFallback.class)
public interface MenuServiceClient {

    @GetMapping("/api/v1/menu/items/{itemId}")
    ApiResponse<MenuItemDTO> getMenuItemById(@PathVariable("itemId") int itemId);
}
