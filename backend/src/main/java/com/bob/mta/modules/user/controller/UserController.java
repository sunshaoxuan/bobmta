package com.bob.mta.modules.user.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.modules.user.dto.UserResponse;
import com.bob.mta.modules.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing user and role management workflows.
 */
@RestController
@RequestMapping(path = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    // TODO: Implement user management endpoints
}