package com.quickbite.auth_service.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adds the UI-selected role to the OAuth2 authorization request.
 *
 * The frontend can call:
 *   /oauth2/authorization/google?role=OWNER
 *
 * We persist that role in the OAuth2 authorization request so the
 * callback flow can create a new Google user with the requested role.
 */
@Component
@RequiredArgsConstructor
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String AUTH_BASE_URI = "/oauth2/authorization";
    private static final String ROLE_PARAM = "role";
    private static final String ORIGIN_PARAM = "origin";
    private static final String ROLE_ATTR = "qb_role";
    private static final String ORIGIN_ATTR = "qb_origin";

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, AUTH_BASE_URI)
                .resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return customize(new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, AUTH_BASE_URI)
                .resolve(request, clientRegistrationId), request);
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest original, HttpServletRequest request) {
        if (original == null) {
            return null;
        }

        String requestedRole = normalizeRole(request.getParameter(ROLE_PARAM));
        if (requestedRole == null) {
            return original;
        }

        String origin = normalizeOrigin(request.getParameter(ORIGIN_PARAM));

        HttpSession session = request.getSession(true);
        session.setAttribute(ROLE_ATTR, requestedRole);
        if (origin != null) {
            session.setAttribute(ORIGIN_ATTR, origin);
        } else {
            session.removeAttribute(ORIGIN_ATTR);
        }

        return OAuth2AuthorizationRequest.from(original)
                .attributes(attrs -> attrs.put(ROLE_ATTR, requestedRole))
                .additionalParameters(params -> params.put(ROLE_ATTR, requestedRole))
                .build();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }

        String upper = role.trim().toUpperCase(Locale.ROOT);
        if ("CUSTOMER".equals(upper) || "OWNER".equals(upper) || "AGENT".equals(upper)) {
            return upper;
        }
        return null;
    }

    private String normalizeOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }

        String lower = origin.trim().toLowerCase(Locale.ROOT);
        if ("login".equals(lower) || "register".equals(lower)) {
            return lower;
        }

        return null;
    }
}
