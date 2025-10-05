package com.bob.mta.modules.user.service.impl;

import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.user.domain.ActivationToken;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.repository.UserRepository;
import com.bob.mta.modules.user.service.UserService;
import com.bob.mta.modules.user.service.command.CreateUserCommand;
import com.bob.mta.modules.user.service.model.ActivationLink;
import com.bob.mta.modules.user.service.model.CreateUserResult;
import com.bob.mta.modules.user.service.model.UserAuthentication;
import com.bob.mta.modules.user.service.model.UserView;
import com.bob.mta.modules.user.service.query.UserQuery;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Duration DEFAULT_ACTIVATION_TTL = Duration.ofHours(24);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Duration activationTtl;

    public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder) {
        this(repository, passwordEncoder, Clock.systemUTC(), DEFAULT_ACTIVATION_TTL);
    }

    public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder, Clock clock,
                           Duration activationTtl) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.activationTtl = activationTtl == null ? DEFAULT_ACTIVATION_TTL : activationTtl;
    }

    @Override
    @Transactional
    public CreateUserResult createUser(CreateUserCommand command) {
        Objects.requireNonNull(command, "command");
        ensureUsernameAvailable(command.username());
        ensureEmailAvailable(command.email());

        String id = UUID.randomUUID().toString();
        Set<String> roles = normalizeRoles(command.roles());
        String passwordHash = passwordEncoder.encode(command.password());
        User user = new User(id, command.username(), command.displayName(), command.email(), passwordHash,
                UserStatus.PENDING_ACTIVATION, roles);
        ActivationToken token = new ActivationToken(generateToken(), expiration());
        user.issueActivationToken(token);
        repository.create(user);
        return new CreateUserResult(toView(user), new ActivationLink(token.token(), token.expiresAt()));
    }

    @Override
    @Transactional
    public ActivationLink resendActivation(String userId) {
        User user = getRequired(userId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_ACTIVE);
        }
        ActivationToken token = new ActivationToken(generateToken(), expiration());
        user.issueActivationToken(token);
        repository.update(user);
        repository.saveActivationToken(user.getId(), token);
        return new ActivationLink(token.token(), token.expiresAt());
    }

    @Override
    @Transactional
    public UserView activateUser(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.ACTIVATION_TOKEN_INVALID);
        }
        User user = repository.findByActivationToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVATION_TOKEN_INVALID));
        ActivationToken activationToken = user.getActivationToken();
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
        repository.update(user);
        repository.deleteActivationToken(user.getId());
        return toView(user);
    }

    @Override
    @Transactional
    public UserView assignRoles(String userId, List<String> roles) {
        User user = getRequired(userId);
        Set<String> normalized = normalizeRoles(roles);
        user.assignRoles(normalized);
        repository.replaceRoles(userId, normalized);
        return toView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserView> listUsers(UserQuery query) {
        UserStatus status = query == null ? null : query.status();
        return repository.findAll(status).stream()
                .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserView getUser(String userId) {
        return toView(getRequired(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthentication authenticate(String username, String password) {
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "auth.invalid_credentials");
        }
        return new UserAuthentication(user.getId(), user.getUsername(), user.getDisplayName(), user.getStatus(),
                new ArrayList<>(user.getRoles()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(toAuthorities(user))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserView getUserByUsername(String username) {
        return repository.findByUsername(username)
                .map(this::toView)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void ensureUsernameAvailable(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (repository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
    }

    private void ensureEmailAvailable(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        Optional<User> existing = repository.findByEmail(email);
        if (existing.isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
    }

    private User getRequired(String userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private UserView toView(User user) {
        return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
                user.getStatus(), user.getRoles().stream().toList());
    }

    private List<GrantedAuthority> toAuthorities(User user) {
        return user.getRoles().stream()
                .filter(StringUtils::hasText)
                .map(role -> new SimpleGrantedAuthority(role.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    private Set<String> normalizeRoles(List<String> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return new LinkedHashSet<>(List.of("ROLE_OPERATOR"));
        }
        return roles.stream()
                .filter(StringUtils::hasText)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Instant expiration() {
        return clock.instant().plus(activationTtl);
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}
