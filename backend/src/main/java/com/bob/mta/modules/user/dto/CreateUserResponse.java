package com.bob.mta.modules.user.dto;

import com.bob.mta.modules.user.service.model.CreateUserResult;

/**
 * Response payload for user creation containing the pending profile and activation link.
 */
public class CreateUserResponse {

    private final UserResponse user;

    private final ActivationLinkResponse activation;

    public CreateUserResponse(final UserResponse user, final ActivationLinkResponse activation) {
        this.user = user;
        this.activation = activation;
    }

    public UserResponse getUser() {
        return user;
    }

    public ActivationLinkResponse getActivation() {
        return activation;
    }

    public static CreateUserResponse from(final CreateUserResult result) {
        return new CreateUserResponse(
                UserResponse.from(result.user()),
                ActivationLinkResponse.from(result.activation()));
    }
}
