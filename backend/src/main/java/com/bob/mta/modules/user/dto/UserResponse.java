package com.bob.mta.modules.user.dto;

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
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.status = status;
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

    public ActivationLinkResponse getActivation() {
        return activation;
    }
}
