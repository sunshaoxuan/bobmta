package com.bob.mta.modules.user.controller;

import com.bob.mta.common.api.ApiResponse;
import com.bob.mta.common.api.PageResponse;
import com.bob.mta.common.exception.BusinessException;
import com.bob.mta.common.exception.ErrorCode;
import com.bob.mta.modules.user.domain.UserStatus;
import com.bob.mta.modules.user.dto.ActivateUserRequest;
import com.bob.mta.modules.user.dto.ActivationLinkResponse;
import com.bob.mta.modules.user.dto.AssignRolesRequest;
import com.bob.mta.modules.user.dto.CreateUserRequest;
import com.bob.mta.modules.user.dto.UserResponse;
import com.bob.mta.modules.user.service.UserService;
import com.bob.mta.modules.user.service.model.CreateUserResult;
import com.bob.mta.modules.user.service.model.UserView;
import com.bob.mta.modules.user.service.query.UserQuery;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody final CreateUserRequest request) {
        final CreateUserResult result = userService.createUser(request.toCommand());
        return ApiResponse.success(UserResponse.from(result.user(), result.activation()));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> listUsers(
            @RequestParam(name = "status", required = false) final UserStatus status,
            @RequestParam(name = "page", defaultValue = "1") final int page,
            @RequestParam(name = "pageSize", defaultValue = "20") final int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pagination.invalid");
        }
        final List<UserView> allUsers = userService.listUsers(new UserQuery(status));
        final int fromIndex = Math.min((page - 1) * pageSize, allUsers.size());
        final int toIndex = Math.min(fromIndex + pageSize, allUsers.size());
        final List<UserResponse> slice = allUsers.subList(fromIndex, toIndex).stream()
                .map(view -> UserResponse.from(view, null))
                .toList();
        return ApiResponse.success(PageResponse.of(slice, allUsers.size(), page, pageSize));
    }

    @PostMapping(path = "/activation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UserResponse> activate(@Valid @RequestBody final ActivateUserRequest request) {
        final UserView user = userService.activateUser(request.getToken());
        return ApiResponse.success(UserResponse.from(user, null));
    }

    @PostMapping(path = "/{userId}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UserResponse> assignRoles(
            @PathVariable("userId") final String userId,
            @Valid @RequestBody final AssignRolesRequest request) {
        final UserView user = userService.assignRoles(userId, request.getRoles());
        return ApiResponse.success(UserResponse.from(user, null));
    }

    @PostMapping(path = "/{userId}/activation/resend")
    public ApiResponse<ActivationLinkResponse> resendActivation(@PathVariable("userId") final String userId) {
        return ApiResponse.success(ActivationLinkResponse.from(userService.resendActivation(userId)));
    }
}
