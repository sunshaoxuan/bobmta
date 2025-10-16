package com.bob.mta.modules.auth.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.auth.service.AuthService;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.repository.UserRepository;
import com.bob.mta.modules.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;

import java.util.Objects;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

@Service
public class DefaultAuthService implements AuthService {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    public DefaultAuthService(JwtTokenProvider tokenProvider,
                              UserService userService,
                              UserRepository userRepository,
                              AuthenticationManager authenticationManager) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
        this.userService = Objects.requireNonNull(userService, "userService");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.authenticationManager = Objects.requireNonNull(authenticationManager, "authenticationManager");
    }

    @Override
    public LoginResponse login(String username, String password) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }

        authenticateWithManager(username, password);

        JwtTokenProvider.GeneratedToken generatedToken =
                tokenProvider.generateToken(user.getId(), user.getUsername(), List.copyOf(user.getRoles()));
        return new LoginResponse(generatedToken.token(), generatedToken.expiresAt(),
                user.getDisplayName(), List.copyOf(user.getRoles()));
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
        return userService.loadUserByUsername(username);
    }

    private void authenticateWithManager(String username, String password) {
        try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (BadCredentialsException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "auth.invalid_credentials", ex);
        }
    }
}
