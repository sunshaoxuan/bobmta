package com.bob.mta.modules.user.service;

import com.bob.mta.modules.user.service.model.UserAuthentication;
import com.bob.mta.modules.user.service.model.UserView;

/**
 * Application service encapsulating user management workflows.
 */
public interface UserService {

    UserAuthentication authenticate(String username, String password);

    UserView loadUserByUsername(String username);
}