package com.quickbite.order.exception;

/**
 * Thrown when a required downstream service (cart-service, restaurant-service)
 * is unavailable during critical operations like order placement.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) { super(message); }
}
