package com.bob.mta.modules.user.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class AssignRolesRequest {

    @NotEmpty
    private List<String> roles;

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
