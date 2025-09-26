package com.bob.mta.modules.user.domain;

<<<<<<< HEAD
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class User {

    private final String id;
    private final String username;
    private final String displayName;
    private final String email;
    private final String passwordHash;
    private final UserStatus status;
    private final Set<String> roles;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    private User(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.displayName = builder.displayName;
        this.email = builder.email;
        this.passwordHash = builder.passwordHash;
        this.status = builder.status;
        this.roles = Collections.unmodifiableSet(new HashSet<>(builder.roles));
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
=======
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregate root modelling a platform user account.
 */
public class User {

    private final String id;

    private final String username;

    private String displayName;

    private String email;

    private String password;

    private UserStatus status;

    private Set<String> roles;

    private ActivationToken activationToken;

    public User(
            final String id,
            final String username,
            final String displayName,
            final String email,
            final String password,
            final UserStatus status,
            final Set<String> roles) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.email = Objects.requireNonNull(email, "email");
        this.password = Objects.requireNonNull(password, "password");
        this.status = Objects.requireNonNull(status, "status");
        this.roles = new LinkedHashSet<>(Objects.requireNonNull(roles, "roles"));
>>>>>>> origin/main
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

<<<<<<< HEAD
    public String getPasswordHash() {
        return passwordHash;
    }

=======
>>>>>>> origin/main
    public UserStatus getStatus() {
        return status;
    }

    public Set<String> getRoles() {
<<<<<<< HEAD
        return roles;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .username(username)
                .displayName(displayName)
                .email(email)
                .passwordHash(passwordHash)
                .status(status)
                .roles(roles)
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String username;
        private String displayName;
        private String email;
        private String passwordHash;
        private UserStatus status;
        private Set<String> roles = new HashSet<>();
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public Builder roles(Set<String> roles) {
            this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
            return this;
        }

        public Builder addRole(String role) {
            Objects.requireNonNull(role, "role");
            this.roles.add(role);
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public User build() {
            return new User(this);
        }
=======
        return Collections.unmodifiableSet(roles);
    }

    public ActivationToken getActivationToken() {
        return activationToken;
    }

    public boolean passwordMatches(final String rawPassword) {
        return Objects.equals(password, rawPassword);
    }

    public ActivationToken issueActivationToken(final String token, final Instant expiresAt) {
        this.activationToken = new ActivationToken(token, expiresAt);
        return activationToken;
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
>>>>>>> origin/main
    }
}
