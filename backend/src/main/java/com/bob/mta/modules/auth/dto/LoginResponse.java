package com.bob.mta.modules.auth.dto;

import java.time.Instant;
import java.util.List;

/**
 * Authentication response carrying issued token and metadata.
 */
public class LoginResponse {

    private final String token;

    private final Instant expiresAt;

    private final String displayName;

    private final List<String> roles;

    public LoginResponse(final String token, final Instant expiresAt, final String displayName, final List<String> roles) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.displayName = displayName;
        this.roles = List.copyOf(roles);
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getRoles() {
        return roles;
    }
}
