package com.quickbite.order.exception;

/**
 * Thrown when customer tries to cancel an order that is
 * already PREPARING, PICKED_UP, DELIVERED, or CANCELLED.
 * As per PDF Section 2.2: "Cancel orders within an allowed time window
 * before preparation begins."
 */
public class OrderCancellationException extends RuntimeException {
    public OrderCancellationException(String message) { super(message); }
}
