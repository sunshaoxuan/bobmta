package com.bob.mta.modules.user.domain;

<<<<<<< HEAD
import java.time.OffsetDateTime;

public class ActivationToken {

    private final String token;
    private final String userId;
    private final OffsetDateTime expiresAt;

    public ActivationToken(String token, String userId, OffsetDateTime expiresAt) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
=======
import java.time.Instant;

/**
 * Represents an activation token issued to a user awaiting verification.
 */
public record ActivationToken(String token, Instant expiresAt) {

    public boolean isExpired(final Instant now) {
        return expiresAt.isBefore(now) || expiresAt.equals(now);
>>>>>>> origin/main
    }
}
