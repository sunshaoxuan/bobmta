package com.bob.mta.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload containing an activation token submitted by the user.
 */
public class ActivateUserRequest {

    @NotBlank
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }
}