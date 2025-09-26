package com.bob.mta.modules.user.service.model;

import java.time.Instant;

/**
 * Value object describing an activation link payload returned to callers.
 */
public record ActivationLink(String token, Instant expiresAt) {
}
