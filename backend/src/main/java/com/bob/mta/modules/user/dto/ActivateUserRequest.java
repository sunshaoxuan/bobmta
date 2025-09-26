package com.bob.mta.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public class ActivateUserRequest {

    @NotBlank
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
