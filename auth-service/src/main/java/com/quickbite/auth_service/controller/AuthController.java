package com.quickbite.auth_service.controller;

import com.quickbite.auth_service.dto.request.*;
import com.quickbite.auth_service.dto.response.*;
import com.quickbite.auth_service.entity.User;
import com.quickbite.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AuthController — REST API for Auth/User Service
 *
 * Base URL: /api/v1/auth
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  ENDPOINT REFERENCE TABLE                                               │
 * ├───────────────────────────────┬──────────┬─────────────────────────────┤
 * │  Endpoint                     │ Auth     │ Called By                   │
 * ├───────────────────────────────┼──────────┼─────────────────────────────┤
 * │  POST /register               │ Public   │ Frontend                    │
 * │  POST /login                  │ Public   │ Frontend                    │
 * │  POST /refresh                │ Public   │ Frontend                    │
 * │  POST /validate-token         │ Public   │ API Gateway + all services  │
 * │  POST /logout                 │ JWT      │ Frontend                    │
 * │  GET  /profile                │ JWT      │ Frontend                    │
 * │  PUT  /profile                │ JWT      │ Frontend                    │
 * │  PUT  /password               │ JWT      │ Frontend                    │
 * │  DELETE /deactivate           │ JWT      │ Frontend                    │
 * │  GET  /users/{id}             │ Public*  │ restaurant/delivery/notif   │
 * │  GET  /users/email/{email}    │ Public*  │ notification-service        │
 * │  GET  /admin/users            │ ADMIN    │ Admin dashboard             │
 * │  GET  /admin/users/role/{role}│ ADMIN    │ Admin dashboard             │
 * │  GET  /admin/users/search     │ ADMIN    │ Admin dashboard             │
 * │  PUT  /admin/users/{id}/suspend│ ADMIN   │ Admin dashboard             │
 * │  PUT  /admin/users/{id}/reactivate│ ADMIN│ Admin dashboard             │
 * │  PUT  /admin/users/{id}/role  │ ADMIN    │ Admin dashboard             │
 * │  DELETE /admin/users/{id}     │ ADMIN    │ Admin dashboard             │
 * └───────────────────────────────┴──────────┴─────────────────────────────┘
 * (* Public but should be on internal network / VPC only in production)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Service", description = "Authentication, authorization & user management")
public class AuthController {

    private final AuthService authService;

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Register new user
     *
     * Request:
     * POST /api/v1/auth/register
     * {
     *   "fullName": "Rahul Sharma",
     *   "email": "rahul@example.com",
     *   "password": "Pass@1234",
     *   "phone": "9876543210",
     *   "role": "CUSTOMER"           ← CUSTOMER | OWNER | AGENT (not ADMIN)
     * }
     *
     * Response 201:
     * {
     *   "success": true,
     *   "message": "Registration successful",
     *   "data": {
     *     "accessToken": "eyJ...",
     *     "tokenType": "Bearer",
     *     "refreshToken": "uuid-...",
     *     "expiresIn": 86400000,
     *     "user": { "userId": 1, "fullName": "Rahul Sharma", "role": "CUSTOMER", ... }
     *   }
     * }
     */
    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    /**
     * Login with email + password
     *
     * Request:
     * POST /api/v1/auth/login
     * { "email": "rahul@example.com", "password": "Pass@1234" }
     *
     * Response 200:
     * { "success": true, "data": { "accessToken": "eyJ...", ... } }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        System.out.println(" LOGIN API HIT");

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Refresh access token using refresh token
     *
     * Request:
     * POST /api/v1/auth/refresh
     * { "refreshToken": "uuid-string" }
     *
     * Response 200: new accessToken (refreshToken stays same)
     */
    @PostMapping("/refresh")
    @Operation(summary = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    /**
     * Validate JWT token — CRITICAL ENDPOINT
     *
     * Called by:
     *  1. API Gateway — validates every incoming request before routing
     *  2. All microservices — when they need to verify token and get user details
     *
     * Request:
     * POST /api/v1/auth/validate-token
     * Header: Authorization: Bearer eyJ...
     *
     * Response 200:
     * {
     *   "valid": true,
     *   "userId": 1,
     *   "email": "rahul@example.com",
     *   "role": "CUSTOMER",
     *   "isActive": true
     * }
     */
    @PostMapping("/validate-token")
    @Operation(summary = "Validate JWT token — used by API Gateway and other microservices")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7) : authHeader;

        TokenValidationResponse response = authService.validateToken(token);
        HttpStatus status = response.getValid() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(response);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AUTHENTICATED USER ENDPOINTS (JWT Required)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Logout — invalidates refresh token
     *
     * POST /api/v1/auth/logout
     * Header: Authorization: Bearer eyJ...
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout — invalidate refresh token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {

        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    /**
     * Get logged-in user's profile
     *
     * GET /api/v1/auth/profile
     * Header: Authorization: Bearer eyJ...
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserResponse response = authService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", response));
    }

    /**
     * Update profile
     *
     * PUT /api/v1/auth/profile
     * Header: Authorization: Bearer eyJ...
     * { "fullName": "Rahul K", "phone": "9876543211", "profilePicUrl": "https://..." }
     */
    @PutMapping("/profile")
    @Operation(summary = "Update user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse response = authService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }

    /**
     * Change password (LOCAL accounts only)
     *
     * PUT /api/v1/auth/password
     * Header: Authorization: Bearer eyJ...
     * { "currentPassword": "old", "newPassword": "new", "confirmNewPassword": "new" }
     */
    @PutMapping("/password")
    @Operation(summary = "Change password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed. Please login again."));
    }

    /**
     * Self-deactivate account
     *
     * DELETE /api/v1/auth/deactivate
     * Header: Authorization: Bearer eyJ...
     */
    @DeleteMapping("/deactivate")
    @Operation(summary = "Deactivate own account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal UserDetails userDetails) {

        authService.deactivateAccount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Account deactivated"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INTERNAL SERVICE-TO-SERVICE ENDPOINTS
    // (Should be restricted to internal network / VPC in production)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get user by ID — called by restaurant-service, delivery-service, notification-service
     *
     * GET /api/v1/auth/users/{id}
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID — internal service call")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Integer id) {
        UserResponse response = authService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User found", response));
    }

    /**
     * Get user by email — called by notification-service
     *
     * GET /api/v1/auth/users/email/{email}
     */
    @GetMapping("/users/email/{email}")
    @Operation(summary = "Get user by email — internal service call")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        UserResponse response = authService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("User found", response));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS — ADMIN role only
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all users (with optional role filter)
     *
     * GET /api/v1/auth/admin/users
     * GET /api/v1/auth/admin/users?role=CUSTOMER
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) User.Role role) {

        List<UserResponse> users = (role != null)
                ? authService.getUsersByRole(role)
                : authService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users fetched", users));
    }

    /**
     * Search users by name
     *
     * GET /api/v1/auth/admin/users/search?name=rahul
     */
    @GetMapping("/admin/users/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users by name", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam String name) {

        List<UserResponse> users = authService.searchUsersByName(name);
        return ResponseEntity.ok(ApiResponse.success("Search results", users));
    }

    /**
     * Suspend user
     *
     * PUT /api/v1/auth/admin/users/{id}/suspend
     */
    @PutMapping("/admin/users/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend user account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> suspendUser(@PathVariable Integer id) {
        authService.suspendUser(id);
        return ResponseEntity.ok(ApiResponse.success("User suspended"));
    }

    /**
     * Reactivate user
     *
     * PUT /api/v1/auth/admin/users/{id}/reactivate
     */
    @PutMapping("/admin/users/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate suspended user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> reactivateUser(@PathVariable Integer id) {
        authService.reactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User reactivated"));
    }

    /**
     * Update user role (e.g. promote CUSTOMER to OWNER or AGENT)
     *
     * PUT /api/v1/auth/admin/users/{id}/role
     * { "role": "OWNER" }
     */
    @PutMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRoleRequest request) {

        UserResponse response = authService.updateUserRole(id, request);
        return ResponseEntity.ok(ApiResponse.success("Role updated. User must login again.", response));
    }

    /**
     * Hard delete user
     *
     * DELETE /api/v1/auth/admin/users/{id}
     */
    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hard delete user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
        authService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted"));
    }
}
