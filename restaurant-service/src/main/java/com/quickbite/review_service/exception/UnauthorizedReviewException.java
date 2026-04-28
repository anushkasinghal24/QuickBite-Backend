package com.quickbite.review_service.exception;
public class UnauthorizedReviewException extends RuntimeException {
    public UnauthorizedReviewException(String message) { super(message); }
}
