package com.quickbite.auth_service;

import com.quickbite.auth_service.dto.request.LoginRequest;
import com.quickbite.auth_service.dto.request.RegisterRequest;
import com.quickbite.auth_service.dto.response.AuthResponse;
import com.quickbite.auth_service.entity.User.Role;
import com.quickbite.auth_service.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * AuthServiceTest — Integration tests for Auth Service
 *
 * Run with: mvn test
 * Requires: MySQL running with quickbite_auth_test database
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    private static final String TEST_EMAIL    = "test_" + System.currentTimeMillis() + "@quickbite.com";
    private static final String TEST_PASSWORD = "Test@1234";
    private static String accessToken;

    @Test
    @Order(1)
    @DisplayName("Register new customer successfully")
    void testRegister() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test User");
        req.setEmail(TEST_EMAIL);
        req.setPassword(TEST_PASSWORD);
        req.setPhone("9876543210");
        req.setRole(Role.CUSTOMER);

        AuthResponse response = authService.register(req);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(response.getUser().getRole()).isEqualTo(Role.CUSTOMER);

        accessToken = response.getAccessToken();
    }

    @Test
    @Order(2)
    @DisplayName("Login with correct credentials")
    void testLogin() {
        LoginRequest req = new LoginRequest();
        req.setEmail(TEST_EMAIL);
        req.setPassword(TEST_PASSWORD);

        AuthResponse response = authService.login(req);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
    }

    @Test
    @Order(3)
    @DisplayName("Validate JWT token returns valid=true")
    void testValidateToken() {
        var result = authService.validateToken(accessToken);

        assertThat(result.getValid()).isTrue();
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @Order(4)
    @DisplayName("Login with wrong password throws exception")
    void testLoginWrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setEmail(TEST_EMAIL);
        req.setPassword("wrongpassword");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @Order(5)
    @DisplayName("Register with duplicate email throws exception")
    void testDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Duplicate User");
        req.setEmail(TEST_EMAIL);
        req.setPassword(TEST_PASSWORD);
        req.setRole(Role.CUSTOMER);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");
    }
}
