package com.quickbite.auth_service.service;

import com.quickbite.auth_service.dto.request.*;
import com.quickbite.auth_service.dto.response.*;
import com.quickbite.auth_service.entity.User;

import java.util.List;

/**
 * AuthService Interface
 *
 * Business contract for all auth operations.
 * Implemented by AuthServiceImpl.
 *
 * Methods grouped by who calls them:
 *
 * [PUBLIC / unauthenticated]
 *   register()          ← anyone
 *   login()             ← anyone
 *   refreshToken()      ← anyone with valid refresh token
 *   validateToken()     ← API Gateway + all microservices
 *
 * [AUTHENTICATED USER]
 *   getProfile()        ← logged-in user
 *   updateProfile()     ← logged-in user
 *   changePassword()    ← logged-in user (LOCAL accounts only)
 *   logout()            ← logged-in user
 *   deactivateAccount() ← logged-in user (self-deactivate)
 *
 * [INTERNAL — service-to-service via Feign / RestTemplate]
 *   getUserById()       ← restaurant-service, delivery-service, notification-service
 *   getUserByEmail()    ← notification-service
 *
 * [ADMIN only]
 *   getAllUsers()        ← admin dashboard
 *   getUsersByRole()    ← admin filter
 *   suspendUser()       ← admin moderation
 *   reactivateUser()    ← admin moderation
 *   deleteUser()        ← admin hard delete
 *   updateUserRole()    ← admin promotes CUSTOMER → OWNER / AGENT
 */
public interface AuthService {

    // ── Public ────────────────────────────────────────────────────────────────

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    TokenValidationResponse validateToken(String token);

    // ── Authenticated User ────────────────────────────────────────────────────

    UserResponse getProfile(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    void logout(String email);

    void deactivateAccount(String email);

    // ── Internal (Service-to-Service) ─────────────────────────────────────────

    UserResponse getUserById(Integer userId);

    UserResponse getUserByEmail(String email);

    // ── Admin ─────────────────────────────────────────────────────────────────

    List<UserResponse> getAllUsers();

    List<UserResponse> getUsersByRole(User.Role role);

    List<UserResponse> searchUsersByName(String name);

    void suspendUser(Integer userId);

    void reactivateUser(Integer userId);

    void deleteUser(Integer userId);

    UserResponse updateUserRole(Integer userId, UpdateRoleRequest request);
}
