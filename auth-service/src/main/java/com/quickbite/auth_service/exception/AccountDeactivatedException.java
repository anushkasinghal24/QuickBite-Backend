package com.quickbite.auth_service.exception;
public class AccountDeactivatedException extends RuntimeException {
    public AccountDeactivatedException(String message) { super(message); }
}
