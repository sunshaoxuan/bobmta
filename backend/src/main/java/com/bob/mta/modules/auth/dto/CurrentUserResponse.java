package com.bob.mta.modules.auth.dto;

import java.util.List;

/**
 * Representation of currently authenticated user for front-end bootstrap.
 */
public class CurrentUserResponse {

    private final String userId;

    private final String username;

    private final String displayName;

    private final List<String> roles;

    public CurrentUserResponse(final String userId, final String username, final String displayName, final List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.roles = List.copyOf(roles);
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getRoles() {
        return roles;
    }
}
