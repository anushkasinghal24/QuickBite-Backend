package com.quickbite.order.exception;

/**
 * Thrown when an illegal order status transition is attempted.
 * e.g. DELIVERED â†’ CONFIRMED, or CANCELLED â†’ PREPARING
 * The lifecycle is strictly: PLACEDâ†’CONFIRMEDâ†’PREPARINGâ†’PICKED_UPâ†’DELIVERED
 */
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String message) { super(message); }
}
