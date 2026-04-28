package com.quickbite.review_service.exception;
public class InvalidReviewException extends RuntimeException {
    public InvalidReviewException(String message) { super(message); }
}
