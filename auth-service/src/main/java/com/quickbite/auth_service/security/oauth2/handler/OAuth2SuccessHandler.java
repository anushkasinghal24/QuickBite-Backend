//package com.quickbite.auth_service.security.oauth2.handler;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.quickbite.auth_service.dto.response.AuthResponse;
//import com.quickbite.auth_service.entity.User;
//import com.quickbite.auth_service.repository.UserRepository;
//import com.quickbite.auth_service.security.jwt.JwtUtils;
//import com.quickbite.auth_service.service.RefreshTokenService;
//import jakarta.servlet.http.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
///**
// * OAuth2SuccessHandler
// *
// * Called after CustomOAuth2UserService successfully processes OAuth2 login.
// *
// * For REST/SPA clients:
// *  → Returns JSON with JWT access token + refresh token
// *
// * For MVC / Thymeleaf (quickbite-web):
// *  → Redirects to frontend URL with token as query param
// *  → Frontend stores token and uses it for subsequent API calls
// */
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
//
//    private final JwtUtils jwtUtils;
//    private final UserRepository userRepository;
//    private final RefreshTokenService refreshTokenService;
//    private final ObjectMapper objectMapper;
//
//    @Override
//    public void onAuthenticationSuccess(HttpServletRequest request,
//                                        HttpServletResponse response,
//                                        Authentication authentication) throws IOException {
//        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
//
//        // Get email from OAuth2 attributes
//        String email = (String) oAuth2User.getAttributes().get("email");
//
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login: " + email));
//
//        // Generate JWT tokens
//        String accessToken  = jwtUtils.generateAccessToken(user);
//        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();
//
//        AuthResponse authResponse = AuthResponse.builder()
//                .accessToken(accessToken)
//                .refreshToken(refreshToken)
//                .expiresIn(jwtUtils.getExpirationMs())
//                .user(AuthResponse.UserSummary.builder()
//                        .userId(user.getUserId())
//                        .fullName(user.getFullName())
//                        .email(user.getEmail())
//                        .phone(user.getPhone())
//                        .role(user.getRole())
//                        .provider(user.getProvider())
//                        .isActive(user.getIsActive())
//                        .profilePicUrl(user.getProfilePicUrl())
//                        .createdAt(user.getCreatedAt())
//                        .build())
//                .build();
//
//        log.info("OAuth2 success — generating JWT for user: {}", email);
//
//        response.setContentType("application/json");
//        response.setCharacterEncoding("UTF-8");
//        response.getWriter().write(objectMapper.writeValueAsString(authResponse));
//    }
//}


package com.quickbite.auth_service.security.oauth2.handler;

import com.quickbite.auth_service.entity.User;
import com.quickbite.auth_service.repository.UserRepository;
import com.quickbite.auth_service.security.jwt.JwtUtils;
import com.quickbite.auth_service.service.RefreshTokenService;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2SuccessHandler — FIXED for Angular SPA
 *
 * After OAuth2 login succeeds, redirects to the Angular frontend's
 * /oauth2/callback route with tokens as query params.
 *
 * Frontend reads the params, stores the tokens, and routes the user.
 *
 * Change from original: instead of writing raw JSON to response,
 * we redirect to:
 *   http://localhost:4200/auth/oauth2/callback?token=<accessToken>&refresh=<refreshToken>
 *
 * In production, replace localhost:4200 with your actual frontend URL.
 * Or better: read it from application.yml as app.frontend-url.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = (String) oAuth2User.getAttributes().get("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login: " + email));

        String accessToken  = jwtUtils.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        log.info("OAuth2 success — redirecting to frontend for user: {}", email);

        // Redirect to Angular frontend with tokens as query params
        String redirectUrl = frontendUrl + "/auth/oauth2/callback"
                + "?token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refresh=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
