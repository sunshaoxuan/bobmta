package com.bob.mta.common.exception;

/**
 * Centralized definition of business error codes aligned with the detailed design specification.
 */
public enum ErrorCode {
    OK(0, "success"),
    BAD_REQUEST(4000, "request.invalid"),
    ACTIVATION_TOKEN_INVALID(4001, "user.activation.invalid"),
    ACTIVATION_TOKEN_EXPIRED(4002, "user.activation.expired"),
    UNAUTHORIZED(4010, "auth.required"),
    FORBIDDEN(4031, "auth.forbidden"),
    USER_INACTIVE(4032, "user.inactive"),
    NOT_FOUND(4040, "resource.not_found"),
    USER_NOT_FOUND(4041, "user.not_found"),
    FILE_NOT_FOUND(4045, "file.not_found"),
    CONFLICT(4090, "operation.conflict"),
    USERNAME_EXISTS(4091, "user.username_exists"),
    EMAIL_EXISTS(4092, "user.email_exists"),
    USER_ALREADY_ACTIVE(4093, "user.already_active"),
    INTERNAL_ERROR(5000, "system.error");

    private final int code;

    private final String defaultMessage;

    ErrorCode(final int code, final String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
