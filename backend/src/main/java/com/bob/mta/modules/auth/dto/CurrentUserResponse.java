package com.bob.mta.modules.auth.dto;

import java.util.List;

public class CurrentUserResponse {

    private final String userId;
    private final String username;
    private final List<String> roles;

    public CurrentUserResponse(String userId, String username, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
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
