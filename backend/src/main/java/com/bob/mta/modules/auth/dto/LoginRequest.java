package com.bob.mta.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login form payload.
 */
public class LoginRequest {

    @NotBlank(message = "username.required")
    private String username;

    @NotBlank(message = "password.required")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}
