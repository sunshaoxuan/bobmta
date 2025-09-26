package com.bob.mta.common.exception;

/**
 * Centralized definition of business error codes aligned with the detailed design specification.
 */
public enum ErrorCode {
    OK(0, "success"),
    BAD_REQUEST(4000, "request.invalid"),
    UNAUTHORIZED(4010, "auth.required"),
    FORBIDDEN(4031, "auth.forbidden"),
    NOT_FOUND(4040, "resource.not_found"),
    CONFLICT(4090, "operation.conflict"),
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
