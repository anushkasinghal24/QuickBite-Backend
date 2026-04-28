package com.quickbite.order.cart.serviceimpl;

import com.quickbite.order.dto.ApiResponse;
import com.quickbite.order.dto.RestaurantDTO;
import com.quickbite.order.cart.dto.*;
import com.quickbite.order.cart.entity.Cart;
import com.quickbite.order.cart.entity.CartItem;
import com.quickbite.order.cart.exception.*;
import com.quickbite.order.cart.feign.MenuServiceClient;
import com.quickbite.order.feign.RestaurantServiceClient;
import com.quickbite.order.cart.repository.CartItemRepository;
import com.quickbite.order.cart.repository.CartRepository;
import com.quickbite.order.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CartServiceImpl
 *
 * Full business logic for cart management as per PDF Section 4.4.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuServiceClient menuServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;

    // â”€â”€â”€ Promo Code Config (hardcoded for now; replace with promo-service later) â”€â”€
    // Format: code â†’ discount percentage
    private static final java.util.Map<String, Double> PROMO_CODES = java.util.Map.of(
            "WELCOME10",  10.0,    // 10% off
            "QUICKBITE20", 20.0,   // 20% off
            "FIRST50",    50.0,    // 50% off first order
            "SAVE15",     15.0     // 15% off
    );

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 1. GET CART BY CUSTOMER
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByCustomer(int customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> createEmptyCart(customerId));
        return mapToCartResponse(cart);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 2. ADD ITEM
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public CartResponse addItem(int customerId, AddItemRequest request) {
        log.info("Adding item {} to cart of customer {}", request.getMenuItemId(), customerId);

        // Step 1: Fetch or create cart
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> createEmptyCart(customerId));

        // Step 2: Validate restaurant switch
        if (cart.getRestaurantId() != null
                && cart.getRestaurantId() != request.getRestaurantId()
                && !cart.isEmpty()) {
            throw new DifferentRestaurantException(
                "Your cart already has items from a different restaurant. " +
                "Please clear your cart before adding items from a new restaurant. " +
                "[RESTAURANT_CONFLICT:" + cart.getRestaurantId() + "]"
            );
        }

        // Step 3: Fetch item from menu-service (with fallback for when menu-service not yet built)
        MenuItemDTO menuItem = fetchMenuItemWithFallback(request.getMenuItemId(), request);

        // Step 4: Validate availability
        if (!menuItem.isAvailable()) {
            throw new ItemNotAvailableException(
                "Item '" + menuItem.getName() + "' is currently out of stock.");
        }

        // Step 5: Validate item belongs to the specified restaurant
        if (menuItem.getRestaurantId() != request.getRestaurantId()) {
            throw new ItemNotAvailableException(
                "Item does not belong to restaurant " + request.getRestaurantId());
        }

        // Step 6: Check if item already in cart â†’ increment quantity
        Optional<CartItem> existingItem = cartItemRepository
                .findByCartCartIdAndMenuItemId(cart.getCartId(), request.getMenuItemId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            log.info("Item already in cart, updated quantity to {}", item.getQuantity());
        } else {
            // Step 7: Create new CartItem with price snapshot
            CartItem newItem = new CartItem();
            newItem.setMenuItemId(menuItem.getItemId());
            newItem.setName(menuItem.getName());
            newItem.setPrice(menuItem.getEffectivePrice());   // snapshot discounted price
            newItem.setQuantity(request.getQuantity());
            newItem.setCustomization(request.getCustomization());
            newItem.setVeg(menuItem.isVeg());
            newItem.setImageUrl(menuItem.getImageUrl());
            newItem.setCart(cart);
            cart.getItems().add(newItem);
        }

        // Step 8: Set restaurantId if cart was empty
        if (cart.getRestaurantId() == null) {
            cart.setRestaurantId(request.getRestaurantId());
        }

        // Step 9: Recalculate total
        cart.recalculateTotal();
        Cart saved = cartRepository.save(cart);

        log.info("Item added successfully. Cart total: {}", saved.getTotalPrice());
        return mapToCartResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 3. REMOVE ITEM
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public CartResponse removeItem(int customerId, int itemId) {
        log.info("Removing item {} from cart of customer {}", itemId, customerId);

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(
                    "Cart not found for customer: " + customerId));

        CartItem itemToRemove = cart.getItems().stream()
                .filter(i -> i.getItemId() == itemId)
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(
                    "Item with id " + itemId + " not found in cart"));

        cart.getItems().remove(itemToRemove);

        // If cart is now empty, reset restaurant lock
        if (cart.isEmpty()) {
            cart.setRestaurantId(null);
            cart.setPromoCode(null);
            cart.setDiscountAmount(0);
        }

        cart.recalculateTotal();
        Cart saved = cartRepository.save(cart);
        return mapToCartResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 4. UPDATE QUANTITY
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public CartResponse updateQuantity(int customerId, int itemId, UpdateQuantityRequest request) {
        log.info("Updating quantity of item {} for customer {} to {}", itemId, customerId, request.getQuantity());

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(
                    "Cart not found for customer: " + customerId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getItemId() == itemId)
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(
                    "Item with id " + itemId + " not found in cart"));

        if (request.getQuantity() == 0) {
            // Remove item if quantity set to 0
            return removeItem(customerId, itemId);
        }

        item.setQuantity(request.getQuantity());
        cart.recalculateTotal();
        Cart saved = cartRepository.save(cart);
        return mapToCartResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 5. CLEAR CART
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public CartResponse clearCart(int customerId) {
        log.info("Clearing cart for customer {}", customerId);

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> createEmptyCart(customerId));

        cart.getItems().clear();
        cart.setRestaurantId(null);
        cart.setPromoCode(null);
        cart.setDiscountAmount(0);
        cart.setTotalPrice(0);
        Cart saved = cartRepository.save(cart);
        return mapToCartResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 6. CART TOTAL
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public double cartTotal(int customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(
                    "Cart not found for customer: " + customerId));
        return cart.getTotalPrice();
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 7. CHANGE RESTAURANT (user confirmed "clear & switch")
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public CartResponse changeRestaurant(int customerId, int newRestaurantId) {
        log.info("Switching restaurant for customer {} to restaurant {}", customerId, newRestaurantId);

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> createEmptyCart(customerId));

        // Clear all items, reset promo, set new restaurant
        cart.getItems().clear();
        cart.setRestaurantId(newRestaurantId);
        cart.setPromoCode(null);
        cart.setDiscountAmount(0);
        cart.setTotalPrice(0);
        Cart saved = cartRepository.save(cart);
        return mapToCartResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 8. APPLY PROMO CODE
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public CartResponse applyPromoCode(int customerId, PromoCodeRequest request) {
        log.info("Applying promo code '{}' for customer {}", request.getPromoCode(), customerId);

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(
                    "Cart not found for customer: " + customerId));

        if (cart.isEmpty()) {
            throw new InvalidPromoCodeException("Cannot apply promo code to an empty cart.");
        }

        String code = request.getPromoCode().toUpperCase().trim();
        Double discountPercent = PROMO_CODES.get(code);

        if (discountPercent == null) {
            throw new InvalidPromoCodeException("Invalid promo code: '" + request.getPromoCode() + "'");
        }

        // Calculate subtotal (without discount)
        double subtotal = cart.getItems().stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        double discountAmount = (subtotal * discountPercent) / 100.0;
        // Round to 2 decimal places
        discountAmount = Math.round(discountAmount * 100.0) / 100.0;

        cart.setPromoCode(code);
        cart.setDiscountAmount(discountAmount);
        cart.recalculateTotal();

        Cart saved = cartRepository.save(cart);
        log.info("Promo applied. Discount: {}. New total: {}", discountAmount, saved.getTotalPrice());
        return mapToCartResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 9. REMOVE PROMO CODE
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public CartResponse removePromoCode(int customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(
                    "Cart not found for customer: " + customerId));

        cart.setPromoCode(null);
        cart.setDiscountAmount(0);
        cart.recalculateTotal();

        Cart saved = cartRepository.save(cart);
        return mapToCartResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 10. GET ALL CARTS (Admin)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public List<CartResponse> getAllCarts() {
        return cartRepository.findAll()
                .stream()
                .map(this::mapToCartResponse)
                .collect(Collectors.toList());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 11. GET CART ENTITY (for order-service internal use)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public Cart getCartEntityByCustomer(int customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException(
                    "Cart not found for customer: " + customerId));
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // PRIVATE HELPERS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    /**
     * Create a new empty cart for a customer.
     */
    private Cart createEmptyCart(int customerId) {
        Cart cart = new Cart();
        cart.setCustomerId(customerId);
        cart.setTotalPrice(0.0);
        cart.setDiscountAmount(0.0);
        return cartRepository.save(cart);
    }

    /**
     * Fetch menu item from menu-service.
     * Falls back to a stub if menu-service is not yet running (during incremental development).
     */
    private MenuItemDTO fetchMenuItemWithFallback(int menuItemId, AddItemRequest request) {
        try {
            ApiResponse<MenuItemDTO> response = menuServiceClient.getMenuItemById(menuItemId);
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("menu-service unavailable, using stub for item {}: {}", menuItemId, e.getMessage());
        }

        // â”€â”€ FALLBACK STUB (remove once menu-service is built) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // This allows cart-service to work standalone during development.
        // The stub trusts whatever price/name the client sends.
        log.warn("Using STUB menu item. Replace with real menu-service call.");
        MenuItemDTO stub = new MenuItemDTO();
        stub.setItemId(menuItemId);
        stub.setRestaurantId(request.getRestaurantId());
        stub.setName("Item #" + menuItemId);
        stub.setPrice(99.0);           // default stub price
        stub.setDiscountedPrice(0);
        stub.setAvailable(true);
        stub.setVeg(false);
        stub.setImageUrl("");
        return stub;
    }

    /**
     * Map Cart entity â†’ CartResponse DTO.
     * Tries to enrich with restaurant name from restaurant-service.
     */
    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        double subtotal = cart.getItems().stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();

        // Try to get restaurant name
        String restaurantName = null;
        if (cart.getRestaurantId() != null) {
            try {
                ApiResponse<RestaurantDTO> rResponse =
                        restaurantServiceClient.getRestaurantById(cart.getRestaurantId());
                if (rResponse != null && rResponse.isSuccess() && rResponse.getData() != null) {
                    restaurantName = rResponse.getData().getName();
                }
            } catch (Exception e) {
                log.warn("Could not fetch restaurant name for id {}", cart.getRestaurantId());
            }
        }

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .customerId(cart.getCustomerId())
                .restaurantId(cart.getRestaurantId())
                .restaurantName(restaurantName)
                .items(itemResponses)
                .itemCount(itemResponses.size())
                .subtotal(Math.round(subtotal * 100.0) / 100.0)
                .discountAmount(cart.getDiscountAmount())
                .totalPrice(Math.round(cart.getTotalPrice() * 100.0) / 100.0)
                .promoCode(cart.getPromoCode())
                .empty(cart.isEmpty())
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .itemId(item.getItemId())
                .menuItemId(item.getMenuItemId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .lineTotal(Math.round(item.getLineTotal() * 100.0) / 100.0)
                .customization(item.getCustomization())
                .veg(item.isVeg())
                .imageUrl(item.getImageUrl())
                .build();
    }
}
