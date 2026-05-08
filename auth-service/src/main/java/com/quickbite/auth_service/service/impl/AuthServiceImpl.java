package com.quickbite.auth_service.service.impl;

import com.quickbite.auth_service.dto.request.*;
import com.quickbite.auth_service.dto.response.*;
import com.quickbite.auth_service.entity.RefreshToken;
import com.quickbite.auth_service.entity.User;
import com.quickbite.auth_service.entity.User.AuthProvider;
import com.quickbite.auth_service.exception.*;
import com.quickbite.auth_service.repository.UserRepository;
import com.quickbite.auth_service.security.jwt.JwtUtils;
import com.quickbite.auth_service.service.AuthService;
import com.quickbite.auth_service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AuthServiceImpl
 *
 * Complete implementation of all auth operations.
 *
 * FUTURE CHANGES when other services are integrated:
 *
 * 1. restaurant-service added:
 *    → getUserById() already exists — restaurant-service Feign will call it.
 *    → No changes needed here.
 *
 * 2. delivery-service added:
 *    → Same as above. Feign calls getUserById().
 *
 * 3. notification-service added:
 *    → getUserByEmail() already exists — no changes needed.
 *
 * 4. API Gateway added:
 *    → validateToken() already exists — Gateway calls it for every request.
 *
 * 5. Admin service / features grow:
 *    → Add more admin methods here (already have CRUD).
 *
 * 6. If you add "Forgot Password" feature:
 *    → Add resetPassword(email) + confirmReset(token, newPassword).
 *    → Add PasswordResetToken entity + repo.
 *    → Integrate with notification-service via RestTemplate/Feign.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository       userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JwtUtils             jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService  refreshTokenService;

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email already registered: " + request.getEmail());
        }

        // 2. Prevent self-assignment of ADMIN role
        if (request.getRole() == User.Role.ADMIN) {
            throw new BadRequestException(
                    "Cannot self-assign ADMIN role. Contact platform administrator.");
        }

        // 3. Check duplicate phone if provided
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException(
                    "Phone number already in use: " + request.getPhone());
        }

        // 4. Build and save user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {} | Role: {}", user.getEmail(), user.getRole());

        // 5. Generate tokens
        String accessToken  = jwtUtils.generateAccessToken(user);
        RefreshToken refresh = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refresh.getToken());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate via Spring Security (checks password + isActive)
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException e) {
            throw new AccountDeactivatedException(
                    "Your account has been suspended. Contact support.");
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Load user from DB
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // 3. Check if user registered via OAuth2 and has no password
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException(
                    "This account uses " + user.getProvider() + " login. " +
                    "Please use the OAuth2 login option.");
        }

        log.info("User logged in: {} | Role: {}", user.getEmail(), user.getRole());

        // 4. Generate tokens
        String accessToken  = jwtUtils.generateAccessToken(user);
        RefreshToken refresh = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refresh.getToken());
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // 1. Find refresh token in DB
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());

        // 2. Verify it's not expired (deletes if expired)
        refreshTokenService.verifyExpiration(refreshToken);

        // 3. Get the user
        User user = refreshToken.getUser();

        if (!user.getIsActive()) {
            throw new AccountDeactivatedException("Account has been deactivated.");
        }

        // 4. Generate new access token (refresh token stays same — no rotation here)
        String newAccessToken = jwtUtils.generateAccessToken(user);

        log.info("Token refreshed for user: {}", user.getEmail());

        return buildAuthResponse(user, newAccessToken, refreshToken.getToken());
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        // 1. Basic validation (signature + expiry)
        if (!jwtUtils.validateToken(token)) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("Token is invalid or expired")
                    .build();
        }

        // 2. Extract claims
        String email  = jwtUtils.extractEmail(token);
        Integer userId = jwtUtils.extractUserId(token);

        // 3. Verify user still exists and is active
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !user.getIsActive()) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message(user == null ? "User not found" : "Account is deactivated")
                    .build();
        }

        return TokenValidationResponse.builder()
                .valid(true)
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .message("Token is valid")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AUTHENTICATED USER OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        User user = findUserByEmail(email);
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUserByEmail(email);

        // Check phone uniqueness if being changed
        if (request.getPhone() != null &&
            !request.getPhone().equals(user.getPhone()) &&
            userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException(
                    "Phone number already in use: " + request.getPhone());
        }

        if (request.getFullName()     != null) user.setFullName(request.getFullName());
        if (request.getPhone()        != null) user.setPhone(request.getPhone());
        if (request.getProfilePicUrl()!= null) user.setProfilePicUrl(request.getProfilePicUrl());

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", email);

        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findUserByEmail(email);

        // OAuth2 users have no local password
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException(
                    "Password change is not available for " + user.getProvider() + " accounts.");
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        // Confirm new passwords match
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("New password and confirm password do not match.");
        }

        // Prevent same password reuse
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all refresh tokens (force re-login)
        refreshTokenService.deleteByUser(user);

        log.info("Password changed for user: {}", email);
    }

    @Override
    @Transactional
    public void logout(String email) {
        User user = findUserByEmail(email);
        refreshTokenService.deleteByUser(user);
        SecurityContextHolder.clearContext();
        log.info("User logged out: {}", email);
    }

    @Override
    @Transactional
    public void deactivateAccount(String email) {
        User user = findUserByEmail(email);
        user.setIsActive(false);
        userRepository.save(user);
        refreshTokenService.deleteByUser(user);
        log.info("Account deactivated by user: {}", email);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INTERNAL — SERVICE-TO-SERVICE
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return UserResponse.fromEntity(findUserByEmail(email));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADMIN OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(User.Role role) {
        return userRepository.findAllByRole(role).stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsersByName(String name) {
        return userRepository.findByFullNameContaining(name).stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void suspendUser(Integer userId) {
        User user = findUserById(userId);
        if (!user.getIsActive()) {
            throw new BadRequestException("User is already suspended.");
        }
        user.setIsActive(false);
        userRepository.save(user);
        refreshTokenService.deleteByUser(user); // Force logout
        log.info("Admin suspended user ID: {}", userId);
    }

    @Override
    @Transactional
    public void reactivateUser(Integer userId) {
        User user = findUserById(userId);
        if (user.getIsActive()) {
            throw new BadRequestException("User is already active.");
        }
        user.setIsActive(true);
        userRepository.save(user);
        log.info("Admin reactivated user ID: {}", userId);
    }

    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        User user = findUserById(userId);
        refreshTokenService.deleteByUser(user);
        userRepository.delete(user);
        log.info("Admin hard-deleted user ID: {}", userId);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Integer userId, UpdateRoleRequest request) {
        throw new BadRequestException("Changing user roles from the admin dashboard is disabled.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .refreshToken(refreshToken)
                .expiresIn(jwtUtils.getExpirationMs())
                .user(AuthResponse.UserSummary.builder()
                        .userId(user.getUserId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .provider(user.getProvider())
                        .isActive(user.getIsActive())
                        .profilePicUrl(user.getProfilePicUrl())
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();
    }
}
