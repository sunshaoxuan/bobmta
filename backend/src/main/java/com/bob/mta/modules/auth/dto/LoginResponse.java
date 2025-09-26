package com.bob.mta.modules.auth.dto;

import java.time.Instant;

/**
 * Authentication response carrying issued token and metadata.
 */
public class LoginResponse {

    private final String token;

    private final Instant expiresAt;

    private final String displayName;

    public LoginResponse(final String token, final Instant expiresAt, final String displayName) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.displayName = displayName;
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
}
