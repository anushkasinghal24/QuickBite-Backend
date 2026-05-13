package com.quickbite.order.cart.exception;

/**
 * Thrown when a customer tries to add an item from a different restaurant
 * than the one already in the cart.
 *
 * As per PDF Section 2.2:
 * "Add items to cart from a single restaurant;
 *  prompted to clear cart when switching restaurants."
 */
public class DifferentRestaurantException extends RuntimeException {
    public DifferentRestaurantException(String message) {
        super(message);
    }
}
