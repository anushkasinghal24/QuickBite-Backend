package com.quickbite.auth_service.security.oauth2.user;

import com.quickbite.auth_service.entity.User.AuthProvider;
import com.quickbite.auth_service.exception.OAuth2AuthenticationException;

import java.util.Map;

/**
 * OAuth2UserInfo — Abstract base for provider-specific user info extraction.
 *
 * Different OAuth2 providers return different attribute structures.
 * This abstraction handles Google and GitHub differences cleanly.
 */
public abstract class OAuth2UserInfo {

    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public abstract String getId();      // OAuth2 subject / ID
    public abstract String getName();    // Display name
    public abstract String getEmail();   // Primary email
    public abstract String getImageUrl(); // Profile picture URL

    // ── Factory ───────────────────────────────────────────────────────────────

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId,
                                                    Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase(AuthProvider.GOOGLE.name())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.GITHUB.name())) {
            return new GithubOAuth2UserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationException(
                    "OAuth2 provider [" + registrationId + "] is not supported.");
        }
    }
}

// ─── Google ───────────────────────────────────────────────────────────────────

class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override public String getId()       { return (String) attributes.get("sub"); }
    @Override public String getName()     { return (String) attributes.get("name"); }
    @Override public String getEmail()    { return (String) attributes.get("email"); }
    @Override public String getImageUrl() { return (String) attributes.get("picture"); }
}

// ─── GitHub ───────────────────────────────────────────────────────────────────

class GithubOAuth2UserInfo extends OAuth2UserInfo {

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override public String getId()       { return String.valueOf(attributes.get("id")); }
    @Override public String getName() {
        String name = (String) attributes.get("name");
        return (name != null && !name.isBlank()) ? name : (String) attributes.get("login");
    }
    @Override public String getEmail()    { return (String) attributes.get("email"); }
    @Override public String getImageUrl() { return (String) attributes.get("avatar_url"); }
}
