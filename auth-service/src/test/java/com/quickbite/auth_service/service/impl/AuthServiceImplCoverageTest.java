package com.quickbite.auth_service.service.impl;

import com.quickbite.auth_service.dto.request.ChangePasswordRequest;
import com.quickbite.auth_service.dto.request.LoginRequest;
import com.quickbite.auth_service.dto.request.RefreshTokenRequest;
import com.quickbite.auth_service.dto.request.UpdateProfileRequest;
import com.quickbite.auth_service.dto.response.AuthResponse;
import com.quickbite.auth_service.dto.response.TokenValidationResponse;
import com.quickbite.auth_service.dto.response.UserResponse;
import com.quickbite.auth_service.entity.RefreshToken;
import com.quickbite.auth_service.entity.User;
import com.quickbite.auth_service.exception.AccountDeactivatedException;
import com.quickbite.auth_service.exception.BadRequestException;
import com.quickbite.auth_service.exception.InvalidCredentialsException;
import com.quickbite.auth_service.exception.InvalidCredentialsException;
import com.quickbite.auth_service.repository.UserRepository;
import com.quickbite.auth_service.security.jwt.JwtUtils;
import com.quickbite.auth_service.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplCoverageTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User localUser;
    private User oauthUser;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        localUser = User.builder()
                .userId(11)
                .fullName("Local User")
                .email("local@quickbite.com")
                .phone("9876543210")
                .role(User.Role.CUSTOMER)
                .provider(User.AuthProvider.LOCAL)
                .isActive(true)
                .passwordHash("hashed-password")
                .build();

        oauthUser = User.builder()
                .userId(12)
                .fullName("OAuth User")
                .email("oauth@quickbite.com")
                .phone("9876543211")
                .role(User.Role.CUSTOMER)
                .provider(User.AuthProvider.GOOGLE)
                .isActive(true)
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .user(localUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        SecurityContextHolder.clearContext();
    }

    @Test
    void login_shouldReturnTokensForLocalAccount() {
        LoginRequest request = new LoginRequest();
        request.setEmail("local@quickbite.com");
        request.setPassword("Password@123");

        Authentication authentication = new UsernamePasswordAuthenticationToken("local", "local");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("local@quickbite.com")).thenReturn(Optional.of(localUser));
        when(jwtUtils.generateAccessToken(localUser)).thenReturn("access-token");
        when(jwtUtils.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenService.createRefreshToken(localUser)).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void login_shouldRejectOAuthAccount() {
        LoginRequest request = new LoginRequest();
        request.setEmail("oauth@quickbite.com");
        request.setPassword("Password@123");

        Authentication authentication = new UsernamePasswordAuthenticationToken("oauth", "oauth");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("oauth@quickbite.com")).thenReturn(Optional.of(oauthUser));

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldTranslateDisabledAccount() {
        LoginRequest request = new LoginRequest();
        request.setEmail("local@quickbite.com");
        request.setPassword("Password@123");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("disabled"));

        assertThrows(AccountDeactivatedException.class, () -> authService.login(request));
    }

    @Test
    void refreshToken_shouldReturnNewAccessToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        when(refreshTokenService.findByToken("refresh-token")).thenReturn(refreshToken);
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(jwtUtils.generateAccessToken(localUser)).thenReturn("new-access");
        when(jwtUtils.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void validateToken_shouldReturnInvalidWhenJwtFails() {
        when(jwtUtils.validateToken("bad-token")).thenReturn(false);

        TokenValidationResponse response = authService.validateToken("bad-token");

        assertFalse(response.getValid());
        assertEquals("Token is invalid or expired", response.getMessage());
    }

    @Test
    void updateProfile_shouldRejectDuplicatePhone() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("9999999999");

        when(userRepository.findByEmail("local@quickbite.com")).thenReturn(Optional.of(localUser));
        when(userRepository.existsByPhone("9999999999")).thenReturn(true);

        assertThrows(com.quickbite.auth_service.exception.DuplicateResourceException.class,
                () -> authService.updateProfile("local@quickbite.com", request));
    }

    @Test
    void changePassword_shouldRejectMismatchedConfirmPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Password@123");
        request.setNewPassword("Password@456");
        request.setConfirmNewPassword("Password@789");

        when(userRepository.findByEmail("local@quickbite.com")).thenReturn(Optional.of(localUser));
        when(passwordEncoder.matches("Password@123", "hashed-password")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> authService.changePassword("local@quickbite.com", request));
    }

    @Test
    void getAllUsers_shouldMapEntitiesToResponses() {
        when(userRepository.findAll()).thenReturn(List.of(localUser, oauthUser));

        List<UserResponse> responses = authService.getAllUsers();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getEmail()).isEqualTo("local@quickbite.com");
        assertThat(responses.get(1).getProvider()).isEqualTo(User.AuthProvider.GOOGLE);
    }
}
