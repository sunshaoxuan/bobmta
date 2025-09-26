package com.bob.mta.modules.user.service;

import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.dto.ActivationLinkResponse;
import com.bob.mta.modules.user.dto.CreateUserRequest;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User createUser(CreateUserRequest request);

    ActivationLinkResponse activateUser(String token);

    ActivationLinkResponse resendActivation(String userId);

    User assignRoles(String userId, List<String> roles);

    List<User> findAll();

    User getById(String id);

    Optional<User> findByUsername(String username);
}
