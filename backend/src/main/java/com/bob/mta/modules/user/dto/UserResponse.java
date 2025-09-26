package com.bob.mta.modules.user.dto;

<<<<<<< HEAD
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
=======
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.service.model.ActivationLink;
import com.bob.mta.modules.user.service.model.UserView;
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

    private final ActivationLinkResponse activation;

    private UserResponse(
            final String id,
            final String username,
            final String displayName,
            final String email,
            final UserStatus status,
            final List<String> roles,
            final ActivationLinkResponse activation) {
>>>>>>> origin/main
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.status = status;
<<<<<<< HEAD
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
=======
        this.roles = new ArrayList<>(roles);
        this.activation = activation;
    }

    public static UserResponse from(final UserView view, final ActivationLink activationLink) {
        final ActivationLinkResponse activation = activationLink == null ? null : ActivationLinkResponse.from(activationLink);
        return new UserResponse(
                view.id(),
                view.username(),
                view.displayName(),
                view.email(),
                view.status(),
                view.roles(),
                activation);
>>>>>>> origin/main
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
<<<<<<< HEAD
        return roles;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
=======
        return new ArrayList<>(roles);
    }

    public ActivationLinkResponse getActivation() {
        return activation;
>>>>>>> origin/main
    }
}
