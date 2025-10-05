package com.bob.mta.modules.user.support;

import com.bob.mta.modules.user.domain.ActivationToken;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.repository.UserRepository;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link UserRepository} for unit tests.
 */
public class FakeUserRepository implements UserRepository {

    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> userIdsByUsername = new ConcurrentHashMap<>();
    private final Map<String, String> userIdsByEmail = new ConcurrentHashMap<>();
    private final Map<String, String> userIdsByToken = new ConcurrentHashMap<>();

    @Override
    public void create(User user) {
        Objects.requireNonNull(user, "user");
        save(copy(user));
    }

    @Override
    public void update(User user) {
        Objects.requireNonNull(user, "user");
        save(copy(user));
    }

    @Override
    public void replaceRoles(String userId, Set<String> roles) {
        User stored = require(userId);
        stored.assignRoles(new LinkedHashSet<>(roles));
    }

    @Override
    public List<User> findAll(UserStatus status) {
        return usersById.values().stream()
                .map(this::copy)
                .filter(user -> status == null || user.getStatus() == status)
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findById(String userId) {
        return Optional.ofNullable(usersById.get(userId)).map(this::copy);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userIdsByUsername.get(normalize(username)))
                .map(usersById::get)
                .map(this::copy);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userIdsByEmail.get(normalize(email)))
                .map(usersById::get)
                .map(this::copy);
    }

    @Override
    public Optional<User> findByActivationToken(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userIdsByToken.get(token))
                .map(usersById::get)
                .map(this::copy);
    }

    @Override
    public void saveActivationToken(String userId, ActivationToken token) {
        User stored = require(userId);
        if (token == null) {
            userIdsByToken.values().removeIf(value -> value.equals(userId));
            stored.clearActivation();
            return;
        }
        userIdsByToken.entrySet().removeIf(entry -> entry.getValue().equals(userId));
        stored.issueActivationToken(token);
        userIdsByToken.put(token.token(), userId);
    }

    @Override
    public void deleteActivationToken(String userId) {
        saveActivationToken(userId, null);
    }

    public void seed(User user) {
        save(copy(user));
    }

    private void save(User user) {
        usersById.put(user.getId(), user);
        userIdsByUsername.put(normalize(user.getUsername()), user.getId());
        userIdsByEmail.put(normalize(user.getEmail()), user.getId());
        if (user.getActivationToken() != null) {
            userIdsByToken.put(user.getActivationToken().token(), user.getId());
        } else {
            userIdsByToken.values().removeIf(value -> value.equals(user.getId()));
        }
    }

    private User require(String userId) {
        User user = usersById.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        return user;
    }

    private User copy(User source) {
        Set<String> roles = new LinkedHashSet<>(source.getRoles());
        User copy = new User(source.getId(), source.getUsername(), source.getDisplayName(), source.getEmail(),
                source.getPassword(), source.getStatus(), roles);
        if (source.getActivationToken() != null) {
            copy.restoreActivationToken(source.getActivationToken());
        }
        return copy;
    }

    private String normalize(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
