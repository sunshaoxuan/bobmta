package com.bob.mta.modules.user.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.modules.user.domain.User;
import com.bob.mta.modules.user.dto.ActivateUserRequest;
import com.bob.mta.modules.user.dto.ActivationLinkResponse;
import com.bob.mta.modules.user.dto.AssignRolesRequest;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.dto.UserResponse;
import com.bob.mta.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return ApiResponse.success(UserResponse.from(user));
    }

    @PostMapping("/activation")
    public ApiResponse<ActivationLinkResponse> activate(@Valid @RequestBody ActivateUserRequest request) {
        ActivationLinkResponse activationLink = userService.activateUser(request.getToken());
        return ApiResponse.success(activationLink);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/activation/resend")
    public ApiResponse<ActivationLinkResponse> resendActivation(@PathVariable String id) {
        ActivationLinkResponse activationLink = userService.resendActivation(id);
        return ApiResponse.success(activationLink);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/roles")
    public ApiResponse<UserResponse> assignRoles(@PathVariable String id, @Valid @RequestBody AssignRolesRequest request) {
        User user = userService.assignRoles(id, request.getRoles());
        return ApiResponse.success(UserResponse.from(user));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping
    public ApiResponse<List<UserResponse>> listUsers() {
        List<UserResponse> responses = userService.findAll().stream()
                .map(UserResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable String id) {
        User user = userService.getById(id);
        return ApiResponse.success(UserResponse.from(user));
    }
}
