package com.bob.mta.modules.auth.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
<<<<<<< HEAD
import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.common.security.JwtUserDetails;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginRequest;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.auth.service.AuthService;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.service.UserService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InMemoryAuthService implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public InMemoryAuthService(UserService userService, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
        }
        List<String> roles = user.getRoles().stream().sorted().toList();
        String token = tokenProvider.createToken(user.getId(), user.getUsername(), roles);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(tokenProvider.getExpirationMinutes());
        return new LoginResponse(token, expiresAt, user.getId(), user.getUsername(), roles);
    }

    @Override
    public CurrentUserResponse currentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
        }
        String userId;
        String username = userDetails.getUsername();
        if (userDetails instanceof JwtUserDetails details) {
            userId = details.getId();
        } else {
            userId = userService.findByUsername(username)
                    .map(User::getId)
                    .orElse("unknown");
        }
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        return new CurrentUserResponse(userId, username, roles);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
        }
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED));
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) () -> role)
                .collect(Collectors.toList());
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.getStatus() == UserStatus.LOCKED)
                .credentialsExpired(false)
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .build();
=======
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

    private record UserRecord(String userId, String username, String displayName, String password, List<String> roles) {
>>>>>>> origin/main
    }
}
