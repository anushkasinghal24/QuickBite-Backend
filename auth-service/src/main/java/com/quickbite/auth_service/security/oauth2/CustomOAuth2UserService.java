package com.quickbite.auth_service.security.oauth2;

import com.quickbite.auth_service.entity.User;
import com.quickbite.auth_service.entity.User.AuthProvider;
import com.quickbite.auth_service.entity.User.Role;
import com.quickbite.auth_service.repository.UserRepository;
import com.quickbite.auth_service.security.oauth2.user.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CustomOAuth2UserService
 *
 * Called by Spring Security after successful Google/GitHub OAuth2 callback.
 *
 * Flow:
 *  1. User clicks "Login with Google/GitHub"
 *  2. Spring Security fetches user info from provider
 *  3. THIS SERVICE processes that info:
 *     a. New user  → create User record with role CUSTOMER (default)
 *     b. Returning user → update profile pic if changed
 *  4. OAuth2SuccessHandler then generates JWT and returns it
 *
 * NOTE: OAuth2 users get CUSTOMER role by default.
 * To become OWNER/AGENT, they must update their role via profile settings.
 * ADMIN role can only be assigned by existing admin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("OAuth2 processing error: {}", ex.getMessage());
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        AuthProvider provider = AuthProvider.valueOf(registrationId);

        OAuth2UserInfo userInfo = OAuth2UserInfo.getOAuth2UserInfo(
                userRequest.getClientRegistration().getRegistrationId(),
                oAuth2User.getAttributes()
        );

        String email = resolveEmail(userRequest, userInfo);
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException(
                    "Email not found from OAuth2 provider. Please use email/password login.");
        }

        // Check if user already exists
        Optional<User> existingByProvider = userRepository
                .findByProviderAndProviderId(provider, userInfo.getId());

        User user;

        if (existingByProvider.isPresent()) {
            // RETURNING OAuth2 USER → update profile pic
            user = existingByProvider.get();
            user.setProfilePicUrl(userInfo.getImageUrl());
            // Update name if changed
            if (userInfo.getName() != null) {
                user.setFullName(userInfo.getName());
            }
        } else {
            // Check if email exists with LOCAL account
            Optional<User> existingByEmail = userRepository.findByEmail(email);

            if (existingByEmail.isPresent()) {
                // Link OAuth2 provider to existing LOCAL account
                user = existingByEmail.get();
                user.setProvider(provider);
                user.setProviderId(userInfo.getId());
                user.setProfilePicUrl(userInfo.getImageUrl());
            } else {
                // BRAND NEW USER → create with CUSTOMER role
                user = User.builder()
                        .fullName(userInfo.getName() != null ? userInfo.getName() : "QuickBite User")
                        .email(email)
                        .provider(provider)
                        .providerId(userInfo.getId())
                        .role(Role.CUSTOMER)  // Default role for OAuth2 signups
                        .isActive(true)
                        .profilePicUrl(userInfo.getImageUrl())
                        .build();
            }
        }

        user = userRepository.save(user);
        log.info("OAuth2 login successful for user: {} via {}", user.getEmail(), provider);

        // Return OAuth2User with our custom attributes
        Map<String, Object> attrs = new HashMap<>(oAuth2User.getAttributes());
        attrs.put("email", user.getEmail());
        if (user.getFullName() != null) {
            attrs.put("name", user.getFullName());
        }
        return new DefaultOAuth2User(
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name())),
                attrs,
                userRequest.getClientRegistration()
                        .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()
        );
    }

    private String resolveEmail(OAuth2UserRequest userRequest, OAuth2UserInfo userInfo) {
        String email = userInfo.getEmail();
        if (email != null && !email.isBlank()) {
            return email;
        }

        if (userRequest.getClientRegistration().getRegistrationId().equalsIgnoreCase("github")) {
            return fetchGithubPrimaryEmail(userRequest);
        }

        return null;
    }

    private String fetchGithubPrimaryEmail(OAuth2UserRequest userRequest) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
            headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Object[]> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    Object[].class
            );

            if (response.getBody() == null || response.getBody().length == 0) {
                return null;
            }

            String fallbackEmail = null;
            for (Object item : response.getBody()) {
                if (!(item instanceof Map<?, ?> rawMap)) {
                    continue;
                }

                Map<String, Object> emailMap = (Map<String, Object>) rawMap;
                String email = (String) emailMap.get("email");
                Boolean primary = (Boolean) emailMap.get("primary");
                Boolean verified = (Boolean) emailMap.get("verified");

                if (email == null || email.isBlank()) {
                    continue;
                }

                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                    return email;
                }

                if (fallbackEmail == null && Boolean.TRUE.equals(verified)) {
                    fallbackEmail = email;
                }

                if (fallbackEmail == null) {
                    fallbackEmail = email;
                }
            }

            return fallbackEmail;
        } catch (Exception ex) {
            log.warn("Unable to fetch GitHub email from /user/emails: {}", ex.getMessage());
            return null;
        }
    }
}
