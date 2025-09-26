package com.bob.mta.modules.auth.service.impl;

import com.bob.mta.common.security.JwtProperties;
import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.auth.service.AuthService;
import com.bob.mta.modules.user.service.UserService;
import com.bob.mta.modules.user.service.model.UserAuthentication;
import com.bob.mta.modules.user.service.model.UserView;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Temporary in-memory auth service that mimics future integration with database-backed identities.
 */
@Service
public class InMemoryAuthService implements AuthService {

    private final JwtTokenProvider tokenProvider;

    private final JwtProperties properties;

    private final UserService userService;

    public InMemoryAuthService(
            final JwtTokenProvider tokenProvider,
            final JwtProperties properties,
            final UserService userService) {
        this.tokenProvider = tokenProvider;
        this.properties = properties;
        this.userService = userService;
    }

    @Override
    public LoginResponse login(final String username, final String password) {
        final UserAuthentication user = userService.authenticate(username, password);
        final Instant expiresAt = Instant.now().plus(properties.getAccessToken().getExpirationMinutes(), ChronoUnit.MINUTES);
        final String primaryRole = user.roles().isEmpty() ? "USER" : user.roles().get(0);
        final String token = tokenProvider.generateToken(user.id(), user.username(), primaryRole);
        return new LoginResponse(token, expiresAt, user.displayName(), user.roles());
    }

    @Override
    public CurrentUserResponse currentUser(final String username) {
        final UserView user = userService.loadUserByUsername(username);
        return new CurrentUserResponse(user.id(), user.username(), user.displayName(), List.copyOf(user.roles()));
    }
}
