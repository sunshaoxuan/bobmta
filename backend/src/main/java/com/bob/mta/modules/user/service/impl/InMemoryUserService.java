package com.bob.mta.modules.user.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.user.domain.ActivationToken;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.dto.ActivationLinkResponse;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class InMemoryUserService implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ActivationToken> activationTokens = new ConcurrentHashMap<>();

    public InMemoryUserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        seedUsers();
    }

    private void seedUsers() {
        registerSeedUser("1", "admin", "Admin", "admin@example.com", "admin123", Set.of("ROLE_ADMIN"), true);
        registerSeedUser("2", "operator", "Operator", "operator@example.com", "operator123", Set.of("ROLE_OPERATOR"), true);
    }

    private void registerSeedUser(String id, String username, String displayName, String email, String rawPassword,
                                  Set<String> roles, boolean activated) {
        OffsetDateTime now = OffsetDateTime.now();
        User user = User.builder()
                .id(id)
                .username(username)
                .displayName(displayName)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(activated ? UserStatus.ACTIVE : UserStatus.PENDING_ACTIVATION)
                .roles(roles)
                .createdAt(now)
                .updatedAt(now)
                .build();
        users.put(user.getId(), user);
    }

    @Override
    public User createUser(CreateUserRequest request) {
        String username = request.getUsername().toLowerCase();
        ensureUsernameNotExists(username);
        String userId = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();
        User user = User.builder()
                .id(userId)
                .username(username)
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .status(UserStatus.PENDING_ACTIVATION)
                .roles(toRoleSet(request.getRoles()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        users.put(user.getId(), user);
        ActivationToken activationToken = generateActivationToken(user.getId());
        activationTokens.put(activationToken.getToken(), activationToken);
        return user;
    }

    @Override
    public ActivationLinkResponse activateUser(String token) {
        ActivationToken activationToken = activationTokens.remove(token);
        if (activationToken == null || activationToken.isExpired()) {
            throw new BusinessException(ErrorCode.ACTIVATION_TOKEN_INVALID);
        }
        User user = users.get(activationToken.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        User updated = user.toBuilder()
                .status(UserStatus.ACTIVE)
                .updatedAt(OffsetDateTime.now())
                .build();
        users.put(updated.getId(), updated);
        return new ActivationLinkResponse(token, activationToken.getExpiresAt());
    }

    @Override
    public ActivationLinkResponse resendActivation(String userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        ActivationToken activationToken = generateActivationToken(userId);
        activationTokens.entrySet().removeIf(entry -> entry.getValue().getUserId().equals(userId));
        activationTokens.put(activationToken.getToken(), activationToken);
        return new ActivationLinkResponse(activationToken.getToken(), activationToken.getExpiresAt());
    }

    @Override
    public User assignRoles(String userId, List<String> roles) {
        User user = users.get(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        User updated = user.toBuilder()
                .roles(toRoleSet(roles))
                .updatedAt(OffsetDateTime.now())
                .build();
        users.put(updated.getId(), updated);
        return updated;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User getById(String id) {
        User user = users.get(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        String normalized = username.toLowerCase();
        return users.values().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(normalized))
                .findFirst();
    }

    private void ensureUsernameNotExists(String username) {
        boolean exists = users.values().stream()
                .map(User::getUsername)
                .anyMatch(existing -> existing.equalsIgnoreCase(username));
        if (exists) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
    }

    private ActivationToken generateActivationToken(String userId) {
        OffsetDateTime expires = OffsetDateTime.now().plus(1, ChronoUnit.DAYS);
        return new ActivationToken(UUID.randomUUID().toString(), userId, expires);
    }

    private Set<String> toRoleSet(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of("ROLE_OPERATOR");
        }
        return roles.stream()
                .filter(StringUtils::hasText)
                .map(role -> role.toUpperCase().startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase())
                .collect(Collectors.toSet());
    }
}
