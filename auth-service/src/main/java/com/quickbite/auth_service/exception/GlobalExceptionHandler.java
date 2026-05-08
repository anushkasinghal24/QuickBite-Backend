package com.quickbite.auth_service.exception;

import com.quickbite.auth_service.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler
 *
 * Catches all exceptions and returns consistent JSON error responses.
 * All error responses follow ApiResponse<T> structure.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDeactivated(AccountDeactivatedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenRefresh(TokenRefreshException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleOAuth2(OAuth2AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: Insufficient permissions"));
    }

    /**
     * Handles @Valid validation failures — returns field-by-field errors.
     *
     * Response example:
     * {
     *   "success": false,
     *   "message": "Validation failed",
     *   "data": {
     *     "email": "Invalid email format",
     *     "password": "Password must be at least 8 characters"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName    = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
//        log.error("Unexpected error: ", ex);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.error("An unexpected error occurred. Please try again."));
//    }



    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleStaticResource(NoResourceFoundException ex) {
        // Don't log as error — this is normal browser behavior
        return ResponseEntity.notFound().build();
    }

//    @ExceptionHandler(NoResourceFoundException.class)
//    public ResponseEntity<Void> handleStaticResource(NoResourceFoundException ex) {
//        log.debug("Static resource not found: {}", ex.getResourcePath());
//        return ResponseEntity.notFound().build();
//    }
}
