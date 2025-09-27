package com.bob.mta.modules.user.dto;

import com.bob.mta.modules.user.domain.UserStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * REST response representing a user profile.
 */
public class UserResponse {

    private final String id;

    private final String username;

    private final String displayName;

    private final String email;

    private final UserStatus status;

    private final List<String> roles;

    public UserResponse(
            final String id,
            final String username,
            final String displayName,
            final String email,
            final UserStatus status,
            final List<String> roles) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.status = status;
        this.roles = new ArrayList<>(roles);
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public List<String> getRoles() {
        return new ArrayList<>(roles);
    }
}