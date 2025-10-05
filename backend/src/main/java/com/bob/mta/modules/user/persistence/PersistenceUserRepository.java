package com.bob.mta.modules.user.persistence;

import com.bob.mta.modules.user.domain.ActivationToken;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@ConditionalOnBean(UserMapper.class)
public class PersistenceUserRepository implements UserRepository {

    private final UserMapper mapper;

    public PersistenceUserRepository(UserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void create(User user) {
        Objects.requireNonNull(user, "user");
        mapper.insertUser(toEntity(user));
        replaceRolesInternal(user.getId(), user.getRoles());
        if (user.getActivationToken() != null) {
            mapper.insertActivationToken(toActivationEntity(user.getId(), user.getActivationToken()));
        }
    }

    @Override
    public void update(User user) {
        Objects.requireNonNull(user, "user");
        mapper.updateUser(toEntity(user));
    }

    @Override
    public void replaceRoles(String userId, Set<String> roles) {
        Objects.requireNonNull(userId, "userId");
        replaceRolesInternal(userId, roles == null ? Set.of() : roles);
    }

    @Override
    public List<User> findAll(UserStatus status) {
        List<UserEntity> entities = mapper.findUsers(status);
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<String> ids = entities.stream().map(UserEntity::id).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, List<String>> rolesByUser = loadRoles(ids);
        Map<String, ActivationToken> tokensByUser = loadActivationTokens(ids);
        return entities.stream()
                .map(entity -> toDomain(entity,
                        rolesByUser.getOrDefault(entity.id(), List.of()),
                        tokensByUser.get(entity.id())))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findById(String userId) {
        Objects.requireNonNull(userId, "userId");
        UserEntity entity = mapper.findById(userId);
        if (entity == null) {
            return Optional.empty();
        }
        List<String> roles = mapper.findRolesByUserId(userId).stream()
                .map(UserRoleEntity::role)
                .collect(Collectors.toCollection(ArrayList::new));
        ActivationTokenEntity token = mapper.findActivationTokenByUserId(userId);
        return Optional.of(toDomain(entity, roles, toDomainToken(token)));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Objects.requireNonNull(username, "username");
        UserEntity entity = mapper.findByUsername(username);
        if (entity == null) {
            return Optional.empty();
        }
        return findById(entity.id());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        Objects.requireNonNull(email, "email");
        UserEntity entity = mapper.findByEmail(email);
        if (entity == null) {
            return Optional.empty();
        }
        return findById(entity.id());
    }

    @Override
    public Optional<User> findByActivationToken(String token) {
        Objects.requireNonNull(token, "token");
        ActivationTokenEntity entity = mapper.findActivationTokenByToken(token);
        if (entity == null) {
            return Optional.empty();
        }
        return findById(entity.userId());
    }

    @Override
    public void saveActivationToken(String userId, ActivationToken token) {
        Objects.requireNonNull(userId, "userId");
        mapper.deleteActivationToken(userId);
        if (token != null) {
            mapper.insertActivationToken(toActivationEntity(userId, token));
        }
    }

    @Override
    public void deleteActivationToken(String userId) {
        Objects.requireNonNull(userId, "userId");
        mapper.deleteActivationToken(userId);
    }

    private void replaceRolesInternal(String userId, Collection<String> roles) {
        mapper.deleteRoles(userId);
        if (roles == null || roles.isEmpty()) {
            return;
        }
        List<UserRoleEntity> entities = roles.stream()
                .filter(Objects::nonNull)
                .map(role -> new UserRoleEntity(userId, role))
                .collect(Collectors.toCollection(ArrayList::new));
        mapper.insertRoles(entities);
    }

    private Map<String, List<String>> loadRoles(Set<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> roles = new LinkedHashMap<>();
        mapper.findRolesByUserIds(userIds).forEach(role ->
                roles.computeIfAbsent(role.userId(), ignored -> new ArrayList<>()).add(role.role()));
        return roles;
    }

    private Map<String, ActivationToken> loadActivationTokens(Set<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<String, ActivationToken> tokens = new LinkedHashMap<>();
        mapper.findActivationTokensByUserIds(userIds)
                .forEach(entity -> tokens.put(entity.userId(), toDomainToken(entity)));
        return tokens;
    }

    private User toDomain(UserEntity entity, Collection<String> roles, ActivationToken token) {
        Set<String> roleSet = roles == null
                ? Set.of()
                : roles.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        User user = new User(entity.id(), entity.username(), entity.displayName(), entity.email(),
                entity.passwordHash(), entity.status(), roleSet);
        if (token != null) {
            user.restoreActivationToken(token);
        }
        return user;
    }

    private ActivationToken toDomainToken(ActivationTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ActivationToken(entity.token(), entity.expiresAt());
    }

    private UserEntity toEntity(User user) {
        return new UserEntity(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
                user.getPassword(), user.getStatus());
    }

    private ActivationTokenEntity toActivationEntity(String userId, ActivationToken token) {
        return new ActivationTokenEntity(userId, token.token(), token.expiresAt());
    }
}
