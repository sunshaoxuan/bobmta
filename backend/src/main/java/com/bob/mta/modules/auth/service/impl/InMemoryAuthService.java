package com.bob.mta.modules.auth.service.impl;

import com.bob.mta.common.security.JwtTokenProvider;
import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginResponse;
import com.bob.mta.modules.auth.service.AuthService;
import com.bob.mta.modules.user.service.UserService;
import com.bob.mta.modules.user.service.model.UserPrincipal;
import com.bob.mta.modules.user.service.model.UserView;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Temporary in-memory auth service that mimics future integration with database-backed identities.
 */
@Service
public class InMemoryAuthService implements AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider tokenProvider;

    private final UserService userService;

    public InMemoryAuthService(
            final AuthenticationManager authenticationManager,
            final JwtTokenProvider tokenProvider,
            final UserService userService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    @Override
    public LoginResponse login(final String username, final String password) {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        final UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        final JwtTokenProvider.GeneratedToken generatedToken = tokenProvider.generateToken(
                principal.getId(),
                principal.getUsername(),
                principal.getRoles());
        return new LoginResponse(
                generatedToken.token(),
                generatedToken.expiresAt(),
                principal.getDisplayName(),
                principal.getRoles());
    }

    @Override
    public CurrentUserResponse currentUser(final String username) {
        final UserView user = userService.getUserByUsername(username);
        return new CurrentUserResponse(user.id(), user.username(), user.displayName(), List.copyOf(user.roles()));
    }
}
