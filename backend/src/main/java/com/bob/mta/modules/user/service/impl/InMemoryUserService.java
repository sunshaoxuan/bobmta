package com.bob.mta.modules.user.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.service.UserService;
import com.bob.mta.modules.user.service.model.UserAuthentication;
import com.bob.mta.modules.user.service.model.UserView;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Simple in-memory implementation of UserService used during early development phases.
 */
@Service
public class InMemoryUserService implements UserService {

    private final Map<String, UserRecord> users = Map.of(
            "admin", new UserRecord("1", "admin", "Admin", "admin@example.com", "admin123", List.of("ADMIN", "OPERATOR")),
            "operator", new UserRecord("2", "operator", "Operator", "operator@example.com", "operator123", List.of("OPERATOR")));

    @Override
    public UserAuthentication authenticate(final String username, final String password) {
        final UserRecord user = users.get(username.toLowerCase());
        if (user == null || !user.password().equals(password)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "auth.invalid_credentials");
        }
        return new UserAuthentication(user.id(), user.username(), user.displayName(), UserStatus.ACTIVE, user.roles());
    }

    @Override
    public UserView loadUserByUsername(final String username) {
        final UserRecord user = users.get(username.toLowerCase());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "user.not_found");
        }
        return new UserView(user.id(), user.username(), user.displayName(), user.email(), UserStatus.ACTIVE, user.roles());
    }

    private record UserRecord(String id, String username, String displayName, String email, String password, List<String> roles) {
    }
}