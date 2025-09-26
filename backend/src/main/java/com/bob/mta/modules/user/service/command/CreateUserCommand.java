package com.bob.mta.modules.user.service.command;

import java.util.List;

/**
 * Command object describing the information required to create a user.
 */
public record CreateUserCommand(
        String username,
        String displayName,
        String email,
        String password,
        List<String> roles) {
}
