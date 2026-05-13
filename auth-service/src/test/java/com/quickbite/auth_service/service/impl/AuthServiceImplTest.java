package com.quickbite.auth_service.service.impl;

import com.quickbite.auth_service.dto.request.RegisterRequest;
import com.quickbite.auth_service.dto.response.AuthResponse;
import com.quickbite.auth_service.entity.RefreshToken;
import com.quickbite.auth_service.entity.User;
import com.quickbite.auth_service.exception.BadRequestException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("test@quickbite.com");
        request.setPassword("Password@123");
        request.setPhone("9876543210");
        request.setRole(User.Role.CUSTOMER);
    }

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        User savedUser = User.builder()
                .userId(101)
                .fullName("Test User")
                .email("test@quickbite.com")
                .phone("9876543210")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .provider(User.AuthProvider.LOCAL)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .user(savedUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(userRepository.existsByEmail("test@quickbite.com")).thenReturn(false);
        when(userRepository.existsByPhone("9876543210")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtils.generateAccessToken(savedUser)).thenReturn("access-token");
        when(jwtUtils.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(refreshToken);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(User.Role.CUSTOMER, response.getUser().getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldRejectAdminSelfAssignment() {
        request.setRole(User.Role.ADMIN);

        when(userRepository.existsByEmail("test@quickbite.com")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }
}
