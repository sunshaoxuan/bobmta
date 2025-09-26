package com.bob.mta.modules.user.service.model;

import com.bob.mta.modules.user.domain.UserStatus;
import java.util.List;

/**
 * Read model exposed by the user service for API consumers.
 */
public record UserView(
        String id,
        String username,
        String displayName,
        String email,
        UserStatus status,
        List<String> roles) {
}
