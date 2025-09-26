package com.bob.mta.modules.user.service.model;

import com.bob.mta.modules.user.domain.UserStatus;
import java.util.List;

/**
 * Read-only view of user for API responses.
 */
public record UserView(String id, String username, String displayName, String email, UserStatus status, List<String> roles) {
}