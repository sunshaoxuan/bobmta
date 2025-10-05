package com.bob.mta.modules.user.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Aggregate root modelling a platform user account.
 */
public class User {

    private final String id;

    private final String username;

    private String displayName;

    private String email;

    private String passwordHash;

    private UserStatus status;

    private Set<String> roles;

    private ActivationToken activationToken;

    public User(
            final String id,
            final String username,
            final String displayName,
            final String email,
            final String passwordHash,
            final UserStatus status,
            final Set<String> roles) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.status = Objects.requireNonNull(status, "status");
        this.roles = new LinkedHashSet<>(Objects.requireNonNull(roles, "roles"));
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

    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public ActivationToken getActivationToken() {
        return activationToken;
    }

    public void issueActivationToken(final ActivationToken token) {
        this.activationToken = Objects.requireNonNull(token, "token");
        markPendingActivation();
    }

    public boolean passwordMatches(final String rawPassword, final PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    public void writePasswordHash(final String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.activationToken = null;
    }

    public void assignRoles(final Set<String> newRoles) {
        this.roles = new LinkedHashSet<>(Objects.requireNonNull(newRoles, "newRoles"));
    }

    public void updateProfile(final String displayName, final String email) {
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.email = Objects.requireNonNull(email, "email");
    }

    public void markSuspended() {
        this.status = UserStatus.SUSPENDED;
    }

    public void markPendingActivation() {
        this.status = UserStatus.PENDING_ACTIVATION;
    }

    public void clearActivation() {
        this.activationToken = null;
    }
}
