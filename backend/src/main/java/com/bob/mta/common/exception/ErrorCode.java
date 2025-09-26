package com.bob.mta.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    UNKNOWN_ERROR("ERR-000", HttpStatus.INTERNAL_SERVER_ERROR, "Unknown error"),
    VALIDATION_ERROR("ERR-001", HttpStatus.BAD_REQUEST, "Validation failed"),
    AUTHENTICATION_FAILED("ERR-100", HttpStatus.UNAUTHORIZED, "Authentication failed"),
    ACCESS_DENIED("ERR-101", HttpStatus.FORBIDDEN, "Access denied"),
    USER_NOT_FOUND("ERR-200", HttpStatus.NOT_FOUND, "User not found"),
    USERNAME_EXISTS("ERR-201", HttpStatus.CONFLICT, "Username already exists"),
    USER_INACTIVE("ERR-202", HttpStatus.BAD_REQUEST, "User inactive"),
    ACTIVATION_TOKEN_INVALID("ERR-203", HttpStatus.BAD_REQUEST, "Invalid activation token"),
    CUSTOMER_NOT_FOUND("ERR-300", HttpStatus.NOT_FOUND, "Customer not found"),
    PLAN_NOT_FOUND("ERR-400", HttpStatus.NOT_FOUND, "Plan not found");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
