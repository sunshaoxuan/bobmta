package com.bob.mta.modules.auth.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.security.JwtProperties;
import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.auth.service.AuthService;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.repository.UserRepository;
import com.bob.mta.modules.user.service.UserService;
import com.bob.mta.modules.user.service.model.UserAuthentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DefaultAuthService implements AuthService {

    private final JwtTokenProvider tokenProvider;
    private final JwtProperties properties;
    private final UserService userService;
    private final UserRepository userRepository;

    public DefaultAuthService(JwtTokenProvider tokenProvider,
                              JwtProperties properties,
                              UserService userService,
                              UserRepository userRepository) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.userService = Objects.requireNonNull(userService, "userService");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
    }

    @Override
    public LoginResponse login(String username, String password) {
        UserAuthentication user = userService.authenticate(username, password);
        Instant expiresAt = Instant.now()
                .plus(properties.getAccessToken().getExpirationMinutes(), ChronoUnit.MINUTES);
        String primaryRole = user.roles().isEmpty() ? "USER" : user.roles().get(0);
        String token = tokenProvider.generateToken(user.id(), user.username(), primaryRole);
        return new LoginResponse(token, expiresAt, user.displayName(), user.roles());
    }

    @Override
    public CurrentUserResponse currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getDisplayName(),
                List.copyOf(user.getRoles()));
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        Collection<? extends GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toList());
        boolean enabled = user.getStatus() == UserStatus.ACTIVE;
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!enabled)
                .build();
    }
}
