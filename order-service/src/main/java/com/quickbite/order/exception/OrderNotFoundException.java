package com.quickbite.order.exception;

/** Thrown when an order is not found by ID */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) { super(message); }
}
