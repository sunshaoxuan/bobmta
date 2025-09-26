package com.bob.mta.modules.auth.dto;

import java.util.List;

<<<<<<< HEAD
public class CurrentUserResponse {

    private final String userId;
    private final String username;
    private final List<String> roles;

    public CurrentUserResponse(String userId, String username, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
=======
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
>>>>>>> origin/main
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

<<<<<<< HEAD
=======
    public String getDisplayName() {
        return displayName;
    }

>>>>>>> origin/main
    public List<String> getRoles() {
        return roles;
    }
}
