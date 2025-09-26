package com.bob.mta.modules.user.service;

<<<<<<< HEAD
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
=======
import com.bob.mta.modules.user.service.command.CreateUserCommand;
import com.bob.mta.modules.user.service.model.ActivationLink;
import com.bob.mta.modules.user.service.model.CreateUserResult;
import com.bob.mta.modules.user.service.model.UserAuthentication;
import com.bob.mta.modules.user.service.model.UserView;
import com.bob.mta.modules.user.service.query.UserQuery;
import java.util.List;

/**
 * Application service encapsulating user management workflows.
 */
public interface UserService {

    CreateUserResult createUser(CreateUserCommand command);

    List<UserView> listUsers(UserQuery query);

    UserView activateUser(String token);

    UserView assignRoles(String userId, List<String> roles);

    ActivationLink resendActivation(String userId);

    UserAuthentication authenticate(String username, String password);

    UserView loadUserByUsername(String username);
>>>>>>> origin/main
}
