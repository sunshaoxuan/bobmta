package com.bob.mta.modules.user.persistence;

/**
 * Association between a user and an assigned role.
 */
public record UserRoleEntity(
        String userId,
        String role
) {
}
