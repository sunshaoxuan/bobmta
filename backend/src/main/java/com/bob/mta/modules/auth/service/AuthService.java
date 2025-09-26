package com.bob.mta.modules.auth.service;

import com.bob.mta.modules.auth.dto.CurrentUserResponse;
import com.bob.mta.modules.auth.dto.LoginRequest;
import com.bob.mta.modules.auth.dto.LoginResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    CurrentUserResponse currentUser(UserDetails userDetails);

    UserDetails loadUserByUsername(String username);
}
