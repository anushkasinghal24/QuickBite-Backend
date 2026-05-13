package com.quickbite.order.exception;

/** Thrown when restaurant is closed, not approved, or below minimum order amount */
public class RestaurantNotAvailableException extends RuntimeException {
    public RestaurantNotAvailableException(String message) { super(message); }
}
