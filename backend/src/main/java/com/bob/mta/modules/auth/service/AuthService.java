package com.bob.mta.modules.auth.service;

import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginResponse;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Authentication related use cases.
 */
public interface AuthService extends UserDetailsService {

    LoginResponse login(String username, String password);

    CurrentUserResponse currentUser(String username);
}
