package com.bob.mta.modules.auth.service;

import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginResponse;

/**
 * Authentication related use cases.
 */
public interface AuthService {

    LoginResponse login(String username, String password);

    CurrentUserResponse currentUser(String username);
}
