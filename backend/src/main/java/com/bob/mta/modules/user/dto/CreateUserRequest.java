package com.bob.mta.modules.user.dto;

<<<<<<< HEAD
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateUserRequest {

    @NotBlank
    @Size(min = 3, max = 32)
    private String username;

    @NotBlank
    private String displayName;

    @Email
    @NotBlank
    private String email;

    private List<String> roles;
=======
import com.bob.mta.modules.user.service.command.CreateUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Payload for creating a new user account.
 */
public class CreateUserRequest {

    @NotBlank
    @Size(max = 64)
    private String username;

    @NotBlank
    @Size(max = 128)
    private String displayName;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(min = 6, max = 128)
    private String password;

    private List<String> roles = new ArrayList<>();
>>>>>>> origin/main

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

    public String getDisplayName() {
        return displayName;
    }

<<<<<<< HEAD
    public void setDisplayName(String displayName) {
=======
    public void setDisplayName(final String displayName) {
>>>>>>> origin/main
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

<<<<<<< HEAD
    public void setEmail(String email) {
        this.email = email;
    }

=======
    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

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
    }

    public CreateUserCommand toCommand() {
        return new CreateUserCommand(username, displayName, email, password, roles);
>>>>>>> origin/main
    }
}
