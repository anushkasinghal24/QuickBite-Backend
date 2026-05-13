package com.quickbite.auth_service.exception;
public class TokenRefreshException extends RuntimeException {
    public TokenRefreshException(String token, String message) {
        super("Refresh token [" + token + "]: " + message);
    }
}
