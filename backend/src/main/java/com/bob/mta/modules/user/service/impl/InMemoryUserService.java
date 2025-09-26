package com.bob.mta.modules.user.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.user.domain.ActivationToken;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
<<<<<<< HEAD
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
=======
import com.bob.mta.modules.user.service.UserService;
import com.bob.mta.modules.user.service.command.CreateUserCommand;
import com.bob.mta.modules.user.service.model.ActivationLink;
import com.bob.mta.modules.user.service.model.CreateUserResult;
import com.bob.mta.modules.user.service.model.UserAuthentication;
import com.bob.mta.modules.user.service.model.UserView;
import com.bob.mta.modules.user.service.query.UserQuery;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
>>>>>>> origin/main
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
<<<<<<< HEAD
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
=======
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Simple in-memory implementation of {@link UserService} used during early development phases.
 */
@Service
public class InMemoryUserService implements UserService {

    private static final Duration ACTIVATION_VALIDITY = Duration.ofHours(24);

    private final Clock clock;

    private final Map<String, User> usersById = new ConcurrentHashMap<>();

    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();

    private final Map<String, User> activationIndex = new ConcurrentHashMap<>();

    public InMemoryUserService() {
        this(Clock.systemUTC());
    }

    public InMemoryUserService(final Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @PostConstruct
    void seedDefaultUsers() {
        registerSeedUser("1", "admin", "Admin", "admin@example.com", "admin123", List.of("ADMIN", "OPERATOR"));
        registerSeedUser("2", "operator", "Operator", "operator@example.com", "operator123", List.of("OPERATOR"));
    }

    @Override
    public synchronized CreateUserResult createUser(final CreateUserCommand command) {
        final String normalizedUsername = normalizeUsername(command.username());
        if (usersByUsername.containsKey(normalizedUsername)) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "user.username_exists");
        }
        final String normalizedEmail = command.email().trim().toLowerCase(Locale.ROOT);
        if (usersById.values().stream().anyMatch(user -> user.getEmail().equalsIgnoreCase(normalizedEmail))) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS, "user.email_exists");
        }
        final Set<String> roles = defaultRoles(command.roles());
        final User user = new User(
                UUID.randomUUID().toString(),
                command.username().trim(),
                command.displayName().trim(),
                normalizedEmail,
                command.password(),
                UserStatus.PENDING_ACTIVATION,
                roles);
        usersById.put(user.getId(), user);
        usersByUsername.put(normalizedUsername, user);
        final ActivationLink activation = issueActivationToken(user);
        return new CreateUserResult(toView(user), activation);
    }

    @Override
    public synchronized List<UserView> listUsers(final UserQuery query) {
        final UserStatus statusFilter = Optional.ofNullable(query).map(UserQuery::status).orElse(null);
        return usersById.values().stream()
                .filter(user -> statusFilter == null || user.getStatus() == statusFilter)
                .sorted(Comparator.comparing(User::getUsername))
                .map(this::toView)
                .toList();
    }

    @Override
    public synchronized UserView activateUser(final String token) {
        final User user = Optional.ofNullable(activationIndex.get(token))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVATION_TOKEN_INVALID, "user.activation.invalid"));
        final ActivationToken activationToken = user.getActivationToken();
        if (activationToken == null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_ACTIVE, "user.already_active");
        }
        if (activationToken.isExpired(Instant.now(clock))) {
            activationIndex.remove(token);
            throw new BusinessException(ErrorCode.ACTIVATION_TOKEN_EXPIRED, "user.activation.expired");
        }
        user.activate();
        activationIndex.remove(token);
        return toView(user);
    }

    @Override
    public synchronized UserView assignRoles(final String userId, final List<String> roles) {
        final User user = usersById.get(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "user.not_found");
        }
        user.assignRoles(defaultRoles(roles));
        return toView(user);
    }

    @Override
    public synchronized ActivationLink resendActivation(final String userId) {
        final User user = usersById.get(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "user.not_found");
        }
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_ACTIVE, "user.already_active");
        }
        return issueActivationToken(user);
    }

    @Override
    public synchronized UserAuthentication authenticate(final String username, final String password) {
        final User user = Optional.ofNullable(usersByUsername.get(normalizeUsername(username)))
                .filter(candidate -> candidate.passwordMatches(password))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "auth.invalid_credentials"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "user.inactive");
        }
        return new UserAuthentication(user.getId(), user.getUsername(), user.getDisplayName(), user.getStatus(),
                new ArrayList<>(user.getRoles()));
    }

    @Override
    public synchronized UserView loadUserByUsername(final String username) {
        final User user = Optional.ofNullable(usersByUsername.get(normalizeUsername(username)))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "user.not_found"));
        return toView(user);
    }

    private ActivationLink issueActivationToken(final User user) {
        final ActivationToken existing = user.getActivationToken();
        if (existing != null) {
            activationIndex.remove(existing.token());
        }
        final Instant expiresAt = Instant.now(clock).plus(ACTIVATION_VALIDITY);
        final ActivationToken token = user.issueActivationToken(UUID.randomUUID().toString(), expiresAt);
        activationIndex.put(token.token(), user);
        return new ActivationLink(token.token(), token.expiresAt());
    }

    private Set<String> defaultRoles(final List<String> roles) {
        final List<String> safeRoles = roles == null ? Collections.emptyList() : roles;
        final Set<String> normalized = safeRoles.stream()
                .filter(Objects::nonNull)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (normalized.isEmpty()) {
            normalized.add("OPERATOR");
        }
        return normalized;
    }

    private String normalizeUsername(final String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private UserView toView(final User user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus(),
                new ArrayList<>(user.getRoles()));
    }

    private void registerSeedUser(
            final String id,
            final String username,
            final String displayName,
            final String email,
            final String password,
            final List<String> roles) {
        final User user = new User(id, username, displayName, email.toLowerCase(Locale.ROOT), password, UserStatus.ACTIVE,
                defaultRoles(roles));
        user.clearActivation();
        usersById.put(user.getId(), user);
        usersByUsername.put(normalizeUsername(username), user);
>>>>>>> origin/main
    }
}
