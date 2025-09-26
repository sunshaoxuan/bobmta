package com.bob.mta.modules.auth.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.security.JwtProperties;
import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.auth.service.AuthService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Temporary in-memory auth service that mimics future integration with database-backed identities.
 */
@Service
public class InMemoryAuthService implements AuthService {

    private final Map<String, UserRecord> users = Map.of(
            "admin", new UserRecord("1", "admin", "Admin", "admin123", List.of("ADMIN", "OPERATOR")),
            "operator", new UserRecord("2", "operator", "Operator", "operator123", List.of("OPERATOR")));

    private final JwtTokenProvider tokenProvider;

    private final JwtProperties properties;

    public InMemoryAuthService(final JwtTokenProvider tokenProvider, final JwtProperties properties) {
        this.tokenProvider = tokenProvider;
        this.properties = properties;
    }

    @Override
    public LoginResponse login(final String username, final String password) {
        final UserRecord user = Optional.ofNullable(users.get(username))
                .filter(record -> record.password().equals(password))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "auth.invalid_credentials"));
        final Instant expiresAt = Instant.now().plus(properties.getAccessToken().getExpirationMinutes(), ChronoUnit.MINUTES);
        final String token = tokenProvider.generateToken(user.userId(), user.username(), user.roles().get(0));
        return new LoginResponse(token, expiresAt, user.displayName());
    }

    @Override
    public CurrentUserResponse currentUser(final String username) {
        final UserRecord user = Optional.ofNullable(users.get(username))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "user.not_found"));
        return new CurrentUserResponse(user.userId(), user.username(), user.displayName(), user.roles());
    }

    private record UserRecord(String userId, String username, String displayName, String password, List<String> roles) {
    }
}
