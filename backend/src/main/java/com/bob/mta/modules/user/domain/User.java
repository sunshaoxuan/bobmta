package com.bob.mta.modules.user.domain;

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Set<String> getRoles() {
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
    }
}
