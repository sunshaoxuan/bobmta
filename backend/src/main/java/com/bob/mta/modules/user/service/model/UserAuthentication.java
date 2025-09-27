package com.bob.mta.modules.user.service.model;

import com.bob.mta.modules.user.domain.UserStatus;
import java.util.List;

/**
 * Authentication context for a verified user.
 */
public record UserAuthentication(String id, String username, String displayName, UserStatus status, List<String> roles) {
}