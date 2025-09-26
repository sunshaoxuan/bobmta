package com.bob.mta.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

<<<<<<< HEAD
=======
/**
 * Payload containing an activation token submitted by the user.
 */
>>>>>>> origin/main
public class ActivateUserRequest {

    @NotBlank
    private String token;

    public String getToken() {
        return token;
    }

<<<<<<< HEAD
    public void setToken(String token) {
=======
    public void setToken(final String token) {
>>>>>>> origin/main
        this.token = token;
    }
}
