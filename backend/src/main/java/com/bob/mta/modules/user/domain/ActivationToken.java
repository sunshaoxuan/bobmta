package com.bob.mta.modules.user.domain;

import java.time.Instant;

/**
 * Represents an activation token issued to a user awaiting verification.
 */
public record ActivationToken(String token, Instant expiresAt) {

    public boolean isExpired(final Instant now) {
        return expiresAt.isBefore(now) || expiresAt.equals(now);
    }
}