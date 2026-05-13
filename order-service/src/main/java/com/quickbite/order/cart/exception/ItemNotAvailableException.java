package com.quickbite.order.cart.exception;

/**
 * Thrown when a menu item is marked out-of-stock (isAvailable = false) in menu-service.
 */
public class ItemNotAvailableException extends RuntimeException {
    public ItemNotAvailableException(String message) {
        super(message);
    }
}
