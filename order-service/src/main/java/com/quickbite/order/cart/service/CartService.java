package com.quickbite.order.cart.service;

import  com.quickbite.order.cart.dto.*;
import  com.quickbite.order.cart.entity.Cart;

import java.util.List;

/**
 * CartService Interface
 *
 * As per PDF Section 4.4 â€” declares:
 *  getCartByCustomer(), addItem(), removeItem(), updateQuantity(),
 *  clearCart(), cartTotal(), changeRestaurant(), applyPromoCode(), getAllCarts()
 */
public interface CartService {

    /**
     * Fetch the active cart for a customer.
     * Creates an empty cart if none exists yet.
     */
    CartResponse getCartByCustomer(int customerId);

    /**
     * Add a menu item to the cart.
     *
     * Rules (from PDF):
     *  - If cart already has items from a DIFFERENT restaurant â†’ throw DifferentRestaurantException
     *    (frontend shows "Your cart has items from X. Clear cart to add from Y?")
     *  - If same menuItemId already in cart â†’ increment quantity instead of adding duplicate
     *  - Calls menu-service to fetch item details & snapshot price
     *  - Validates isAvailable = true before adding
     */
    CartResponse addItem(int customerId, AddItemRequest request);

    /**
     * Remove a specific CartItem by its itemId.
     */
    CartResponse removeItem(int customerId, int itemId);

    /**
     * Update the quantity of a specific CartItem.
     * If quantity becomes 0 â†’ remove the item.
     */
    CartResponse updateQuantity(int customerId, int itemId, UpdateQuantityRequest request);

    /**
     * Clear all items from cart and reset restaurantId, promoCode, discount.
     * Called when:
     *  - User explicitly clears cart
     *  - User switches restaurant (changeRestaurant)
     *  - Order is successfully placed (called by order-service)
     */
    CartResponse clearCart(int customerId);

    /**
     * Calculate and return the current cart total.
     * Returns subtotal (before discount), discountAmount, and finalTotal.
     */
    double cartTotal(int customerId);

    /**
     * Switch restaurant: clears all existing items and sets new restaurantId.
     * Called when user confirms "Yes, clear cart" on the DifferentRestaurant prompt.
     */
    CartResponse changeRestaurant(int customerId, int newRestaurantId);

    /**
     * Apply a promo code to the cart.
     *
     * Current implementation: hardcoded promo codes for demo.
     * Future: will call a promo-service via Feign for validation.
     *
     * As per PDF Section 2.2: "apply promo codes for discounts"
     */
    CartResponse applyPromoCode(int customerId, PromoCodeRequest request);

    /**
     * Remove applied promo code from cart.
     */
    CartResponse removePromoCode(int customerId);

    /**
     * Admin use: get all carts across the platform.
     * As per PDF Section 4.4: getAllCarts()
     */
    List<CartResponse> getAllCarts();

    /**
     * Get the raw Cart entity â€” used internally by order-service (via Feign)
     * to snapshot cart items when placing an order.
     */
    Cart getCartEntityByCustomer(int customerId);
}
