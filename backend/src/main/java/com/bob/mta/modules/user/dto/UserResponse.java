package com.bob.mta.modules.user.dto;

import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;

import java.time.OffsetDateTime;
import java.util.List;

public class UserResponse {

    private final String id;
    private final String username;
    private final String displayName;
    private final String email;
    private final UserStatus status;
    private final List<String> roles;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public UserResponse(String id, String username, String displayName, String email, UserStatus status,
                        List<String> roles, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.status = status;
        this.roles = roles;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus(),
                user.getRoles().stream().sorted().toList(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
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
        return roles;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
