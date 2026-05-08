package com.quickbite.order.exception;

/** Thrown when customer tries to place an order with an empty cart */
public class EmptyCartException extends RuntimeException {
    public EmptyCartException(String message) { super(message); }
}
