package com.bob.mta.modules.user.repository;

import com.bob.mta.modules.user.domain.ActivationToken;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.domain.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Abstraction over user persistence operations.
 */
public interface UserRepository {

    void create(User user);

    void update(User user);

    void replaceRoles(String userId, Set<String> roles);

    List<User> findAll(UserStatus status);

    Optional<User> findById(String userId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByActivationToken(String token);

    void saveActivationToken(String userId, ActivationToken token);

    void deleteActivationToken(String userId);
}
