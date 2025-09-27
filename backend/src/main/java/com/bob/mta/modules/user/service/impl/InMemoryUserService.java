package com.bob.mta.modules.user.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.user.domain.ActivationToken;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.i18n.Localization;
import com.bob.mta.i18n.LocalizationKeys;
import com.bob.mta.modules.user.service.UserService;
import com.bob.mta.modules.user.service.command.CreateUserCommand;
import com.bob.mta.modules.user.service.model.ActivationLink;
import com.bob.mta.modules.user.service.model.CreateUserResult;
import com.bob.mta.modules.user.service.model.UserAuthentication;
import com.bob.mta.modules.user.service.model.UserView;
import com.bob.mta.modules.user.service.query.UserQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Simple in-memory implementation of {@link UserService} used during early development phases.
 */
@Service
public class InMemoryUserService implements UserService {

    private static final Duration DEFAULT_ACTIVATION_TTL = Duration.ofHours(24);

    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();
    private final Clock clock;
    private final Duration activationTtl;

    public InMemoryUserService() {
        this(Clock.systemUTC(), DEFAULT_ACTIVATION_TTL);
    }

    public InMemoryUserService(final Clock clock) {
        this(clock, DEFAULT_ACTIVATION_TTL);
    }

    public InMemoryUserService(final Clock clock, final Duration activationTtl) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.activationTtl = Objects.requireNonNull(activationTtl, "activationTtl");
        seedDefaultUsers();
    }

    public void seedDefaultUsers() {
        usersById.clear();
        usersByUsername.clear();
        idSequence.set(0);
        registerActiveUser(nextId(), "admin", "系统管理员", "admin@example.com", "admin123",
                List.of("ADMIN", "OPERATOR"));
        registerActiveUser(nextId(), "operator", "运维专员", "operator@example.com", "operator123",
                List.of("OPERATOR"));
    }

    @Override
    public CreateUserResult createUser(final CreateUserCommand command) {
        Objects.requireNonNull(command, "command");
        ensureUsernameAvailable(command.username());
        ensureEmailAvailable(command.email());

        final String id = nextId();
        final Set<String> roles = normalizeRoles(command.roles());
        final User user = new User(
                id,
                command.username(),
                command.displayName(),
                command.email(),
                command.password(),
                UserStatus.PENDING_ACTIVATION,
                roles);
        final ActivationToken token = new ActivationToken(generateToken(), expiration());
        user.issueActivationToken(token);
        save(user);
        return new CreateUserResult(toView(user), new ActivationLink(token.token(), token.expiresAt()));
    }

    @Override
    public ActivationLink resendActivation(final String userId) {
        final User user = getRequiredById(userId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_ACTIVE);
        }
        final ActivationToken token = new ActivationToken(generateToken(), expiration());
        user.issueActivationToken(token);
        return new ActivationLink(token.token(), token.expiresAt());
    }

    @Override
    public UserView activateUser(final String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.ACTIVATION_TOKEN_INVALID);
        }
        final User user = findByActivationToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVATION_TOKEN_INVALID));
        final ActivationToken activationToken = user.getActivationToken();
        if (activationToken == null) {
            throw new BusinessException(ErrorCode.ACTIVATION_TOKEN_INVALID);
        }
        if (activationToken.isExpired(clock.instant())) {
            throw new BusinessException(ErrorCode.ACTIVATION_TOKEN_EXPIRED);
        }
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_ACTIVE);
        }
        user.activate();
        user.clearActivation();
        return toView(user);
    }

    @Override
    public UserView assignRoles(final String userId, final List<String> roles) {
        final User user = getRequiredById(userId);
        final Set<String> normalized = normalizeRoles(roles);
        user.assignRoles(normalized);
        return toView(user);
    }

    @Override
    public List<UserView> listUsers(final UserQuery query) {
        return usersById.values().stream()
                .sorted(Comparator.comparing(User::getUsername))
                .filter(user -> query == null || query.status() == null || user.getStatus() == query.status())
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public UserView getUser(final String userId) {
        return toView(getRequiredById(userId));
    }

    @Override
    public UserAuthentication authenticate(final String username, final String password) {
        final User user = getRequiredByUsername(username);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        if (!user.passwordMatches(password)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "auth.invalid_credentials");
        }
        return new UserAuthentication(user.getId(), user.getUsername(), user.getDisplayName(), user.getStatus(),
                new ArrayList<>(user.getRoles()));
    }

    @Override
    public UserView loadUserByUsername(final String username) {
        return toView(getRequiredByUsername(username));
    }

    private void registerActiveUser(
            final String id,
            final String username,
            final String displayName,
            final String email,
            final String password,
            final List<String> roles) {
        final User user = new User(id, username, displayName, email, password, UserStatus.ACTIVE, normalizeRoles(roles));
        user.clearActivation();
        save(user);
    }

    private void ensureUsernameAvailable(final String username) {
        final String key = normalizeUsername(username);
        if (usersByUsername.containsKey(key)) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
    }

    private void ensureEmailAvailable(final String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        final boolean exists = usersById.values().stream()
                .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
        if (exists) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
    }

    private User getRequiredById(final String userId) {
        final User user = usersById.get(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private User getRequiredByUsername(final String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        final User user = usersByUsername.get(normalizeUsername(username));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private Optional<User> findByActivationToken(final String token) {
        return usersById.values().stream()
                .filter(user -> user.getActivationToken() != null)
                .filter(user -> token.equals(user.getActivationToken().token()))
                .findFirst();
    }

    private void save(final User user) {
        usersById.put(user.getId(), user);
        usersByUsername.put(normalizeUsername(user.getUsername()), user);
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

    private String normalizeUsername(final String username) {
        return Objects.requireNonNull(username, "username").toLowerCase(Locale.ROOT);
    }

    private Set<String> normalizeRoles(final List<String> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return new LinkedHashSet<>(List.of("ROLE_OPERATOR"));
        }
        return roles.stream()
                .filter(StringUtils::hasText)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String nextId() {
        return Long.toString(idSequence.incrementAndGet());
    }

    private Instant expiration() {
        return clock.instant().plus(activationTtl);
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}
