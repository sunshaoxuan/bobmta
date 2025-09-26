package com.bob.mta.common.exception;

<<<<<<< HEAD
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

=======
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

>>>>>>> origin/main
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
