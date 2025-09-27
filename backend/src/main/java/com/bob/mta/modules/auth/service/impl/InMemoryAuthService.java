package com.bob.mta.modules.auth.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
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
    }
}
