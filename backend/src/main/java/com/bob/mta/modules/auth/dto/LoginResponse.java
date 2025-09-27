package com.bob.mta.modules.auth.dto;

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
    }

    public String getToken() {
        return token;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }
}
