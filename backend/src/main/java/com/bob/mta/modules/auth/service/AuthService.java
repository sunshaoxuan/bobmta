package com.bob.mta.modules.auth.service;

import com.bob.mta.modules.auth.dto.CurrentUserResponse;
<<<<<<< HEAD
import com.bob.mta.modules.auth.dto.LoginRequest;
import com.bob.mta.modules.auth.dto.LoginResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    CurrentUserResponse currentUser(UserDetails userDetails);

    UserDetails loadUserByUsername(String username);
=======
import com.bob.mta.modules.auth.dto.LoginResponse;

/**
 * Authentication related use cases.
 */
public interface AuthService {

    LoginResponse login(String username, String password);

    CurrentUserResponse currentUser(String username);
>>>>>>> origin/main
}
