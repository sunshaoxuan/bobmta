package com.bob.mta.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

<<<<<<< HEAD
public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
=======
/**
 * Login form payload.
 */
public class LoginRequest {

    @NotBlank(message = "username.required")
    private String username;

    @NotBlank(message = "password.required")
>>>>>>> origin/main
    private String password;

    public String getUsername() {
        return username;
    }

<<<<<<< HEAD
    public void setUsername(String username) {
=======
    public void setUsername(final String username) {
>>>>>>> origin/main
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

<<<<<<< HEAD
    public void setPassword(String password) {
=======
    public void setPassword(final String password) {
>>>>>>> origin/main
        this.password = password;
    }
}
