package com.quickbite.auth_service.security.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Redirects OAuth2 failures back to the Angular app with a readable message.
 */
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String ORIGIN_ATTR = "qb_origin";

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String origin = resolveOrigin(request.getSession(false));
        String message = exception.getMessage() != null ? exception.getMessage() : "OAuth sign in failed.";

        String redirectUrl = frontendUrl + "/" + origin
                + "?oauthError=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String resolveOrigin(HttpSession session) {
        if (session == null) {
            return "login";
        }

        Object value = session.getAttribute(ORIGIN_ATTR);
        if (value instanceof String origin) {
            String normalized = origin.trim().toLowerCase();
            if ("register".equals(normalized)) {
                return "register";
            }
        }

        return "login";
    }
}
