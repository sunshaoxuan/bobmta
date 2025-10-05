package com.bob.mta.modules.user.service;

import com.bob.mta.modules.user.service.command.CreateUserCommand;
import com.bob.mta.modules.user.service.model.ActivationLink;
import com.bob.mta.modules.user.service.model.CreateUserResult;
import com.bob.mta.modules.user.service.model.UserAuthentication;
import com.bob.mta.modules.user.service.model.UserView;
import com.bob.mta.modules.user.service.query.UserQuery;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Application service encapsulating user management workflows.
 */
public interface UserService extends UserDetailsService {

    CreateUserResult createUser(CreateUserCommand command);

    ActivationLink resendActivation(String userId);

    UserView activateUser(String token);

    UserView assignRoles(String userId, List<String> roles);

    List<UserView> listUsers(UserQuery query);

    UserView getUser(String userId);

    UserAuthentication authenticate(String username, String password);

    UserView getUserByUsername(String username);
}