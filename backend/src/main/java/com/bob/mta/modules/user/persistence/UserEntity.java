package com.bob.mta.modules.user.persistence;

import com.bob.mta.modules.user.domain.UserStatus;

/**
 * Database representation of a user account.
 */
public record UserEntity(
        String id,
        String username,
        String displayName,
        String email,
        String passwordHash,
        UserStatus status
) {
}
