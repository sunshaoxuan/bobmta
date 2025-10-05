package com.bob.mta.modules.user.persistence;

import java.time.Instant;

/**
 * Stored activation token for a user awaiting verification.
 */
public record ActivationTokenEntity(
        String userId,
        String token,
        Instant expiresAt
) {
}
