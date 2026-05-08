package com.quickbite.review_service.exception;
public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(String message) { super(message); }
}
