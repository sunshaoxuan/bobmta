package com.bob.mta.modules.user.service.query;

import com.bob.mta.modules.user.domain.UserStatus;

/**
 * Filtering options available when listing users.
 */
public record UserQuery(UserStatus status) {
}
