package com.quickbite.order.cart.serviceimpl;

import com.quickbite.order.cart.dto.AddItemRequest;
import com.quickbite.order.cart.dto.MenuItemDTO;
import com.quickbite.order.cart.entity.Cart;
import com.quickbite.order.cart.entity.CartItem;
import com.quickbite.order.cart.feign.MenuServiceClient;
import com.quickbite.order.cart.repository.CartItemRepository;
import com.quickbite.order.cart.repository.CartRepository;
import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.RestaurantDTO;
import com.quickbite.order.feign.RestaurantServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private MenuServiceClient menuServiceClient;
    @Mock private RestaurantServiceClient restaurantServiceClient;

    @InjectMocks private CartServiceImpl cartService;

    @Test
    void addItem_switchingRestaurants_clearsPreviousCartItems() {
        Cart cart = new Cart();
        cart.setCartId(11);
        cart.setCustomerId(77);
        cart.setRestaurantId(1);
        cart.setDiscountAmount(15.0);
        cart.setPromoCode("SAVE15");

        CartItem oldItem = new CartItem();
        oldItem.setItemId(101);
        oldItem.setMenuItemId(101);
        oldItem.setName("Old Burger");
        oldItem.setPrice(120.0);
        oldItem.setQuantity(1);
        oldItem.setCart(cart);
        cart.getItems().add(oldItem);
        cart.recalculateTotal();

        MenuItemDTO newItem = new MenuItemDTO();
        newItem.setItemId(202);
        newItem.setRestaurantId(2);
        newItem.setName("Fresh Pizza");
        newItem.setPrice(150.0);
        newItem.setDiscountedPrice(120.0);
        newItem.setAvailable(true);

        RestaurantDTO restaurant = new RestaurantDTO();
        restaurant.setRestaurantId(2);
        restaurant.setName("Pizza House");
        restaurant.setOpen(true);
        restaurant.setApproved(true);

        when(cartRepository.findByCustomerId(77)).thenReturn(Optional.of(cart));
        when(menuServiceClient.getMenuItemById(202)).thenReturn(ApiResponse.success("ok", newItem));
        when(restaurantServiceClient.getRestaurantById(2)).thenReturn(ApiResponse.success("ok", restaurant));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddItemRequest request = AddItemRequest.builder()
                .menuItemId(202)
                .restaurantId(2)
                .quantity(1)
                .build();

        var response = cartService.addItem(77, request);

        assertEquals(2, response.getRestaurantId());
        assertEquals(1, response.getItemCount());
        assertEquals("Fresh Pizza", response.getItems().get(0).getName());
        assertEquals(120.0, response.getSubtotal(), 0.01);
        assertEquals(120.0, response.getTotalPrice(), 0.01);
        assertEquals(null, response.getPromoCode());

        verify(cartItemRepository).deleteByCartCartId(11);
        verify(cartRepository).save(any(Cart.class));
    }
}
