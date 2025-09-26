package com.bob.mta.modules.user.dto;

import jakarta.validation.constraints.NotEmpty;
<<<<<<< HEAD

import java.util.List;

public class AssignRolesRequest {

    @NotEmpty
    private List<String> roles;
=======
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Payload for updating user roles.
 */
public class AssignRolesRequest {

    @NotEmpty
    private List<@Size(min = 2, max = 64) String> roles = new ArrayList<>();
>>>>>>> origin/main

    public List<String> getRoles() {
        return roles;
    }

<<<<<<< HEAD
    public void setRoles(List<String> roles) {
        this.roles = roles;
=======
    public void setRoles(final List<String> roles) {
        this.roles = roles == null ? new ArrayList<>() : new ArrayList<>(roles);
>>>>>>> origin/main
    }
}
