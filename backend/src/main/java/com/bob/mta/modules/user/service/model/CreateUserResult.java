package com.bob.mta.modules.user.service.model;

/**
 * Combined result of a user creation attempt containing the new profile and activation link.
 */
public record CreateUserResult(UserView user, ActivationLink activation) {
}
