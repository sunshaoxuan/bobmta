package com.bob.mta.modules.auth.dto;

<<<<<<< HEAD
import java.time.OffsetDateTime;
import java.util.List;

public class LoginResponse {

    private final String token;
    private final OffsetDateTime expiresAt;
    private final String userId;
    private final String username;
    private final List<String> roles;

    public LoginResponse(String token, OffsetDateTime expiresAt, String userId, String username, List<String> roles) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.username = username;
        this.roles = roles;
=======
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
>>>>>>> origin/main
    }

    public String getToken() {
        return token;
    }

<<<<<<< HEAD
    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
=======
    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getDisplayName() {
        return displayName;
>>>>>>> origin/main
    }

    public List<String> getRoles() {
        return roles;
    }
}
