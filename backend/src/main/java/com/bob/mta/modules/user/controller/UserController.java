package com.bob.mta.modules.user.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.dto.ActivateUserRequest;
import com.bob.mta.modules.user.dto.ActivationLinkResponse;
import com.bob.mta.modules.user.dto.AssignRolesRequest;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.dto.CreateUserResponse;
import com.bob.mta.modules.user.dto.UserResponse;
import com.bob.mta.modules.user.service.UserService;
import com.bob.mta.modules.user.service.command.CreateUserCommand;
import com.bob.mta.modules.user.service.model.ActivationLink;
import com.bob.mta.modules.user.service.model.CreateUserResult;
import com.bob.mta.modules.user.service.model.UserView;
import com.bob.mta.modules.user.service.query.UserQuery;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping
    public ApiResponse<List<UserResponse>> listUsers(@RequestParam(required = false) final UserStatus status) {
        final List<UserResponse> users = userService.listUsers(new UserQuery(status)).stream()
                .map(UserResponse::from)
                .toList();
        return ApiResponse.success(users);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable("id") final String id) {
        final UserView user = userService.getUser(id);
        return ApiResponse.success(UserResponse.from(user));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<CreateUserResponse> createUser(@Valid @RequestBody final CreateUserRequest request) {
        final CreateUserCommand command = new CreateUserCommand(
                request.getUsername(),
                request.getDisplayName(),
                request.getEmail(),
                request.getPassword(),
                request.getRoles());
        final CreateUserResult result = userService.createUser(command);
        return ApiResponse.success(CreateUserResponse.from(result));
    }

    @PostMapping(path = "/{id}/activation/resend")
    public ApiResponse<ActivationLinkResponse> resendActivation(@PathVariable("id") final String id) {
        final ActivationLink activation = userService.resendActivation(id);
        return ApiResponse.success(ActivationLinkResponse.from(activation));
    }

    @PostMapping(path = "/activation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UserResponse> activate(@Valid @RequestBody final ActivateUserRequest request) {
        final UserView user = userService.activateUser(request.getToken());
        return ApiResponse.success(UserResponse.from(user));
    }

    @PutMapping(path = "/{id}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UserResponse> assignRoles(
            @PathVariable("id") final String id,
            @Valid @RequestBody final AssignRolesRequest request) {
        final UserView user = userService.assignRoles(id, request.getRoles());
        return ApiResponse.success(UserResponse.from(user));
    }
}
