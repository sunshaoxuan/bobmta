package com.bob.mta.common.api;

import com.bob.mta.common.exception.ErrorCode;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Generic wrapper for REST responses to enforce a consistent envelope structure.
 *
 * @param <T> payload type wrapped by the response
 */
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    private final String message;

    private final T data;

    private ApiResponse(final int code, final String message, final T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(final T data) {
        return new ApiResponse<>(ErrorCode.OK.getCode(), ErrorCode.OK.getDefaultMessage(), data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ErrorCode.OK.getCode(), ErrorCode.OK.getDefaultMessage(), null);
    }

    public static ApiResponse<Void> failure(final ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getDefaultMessage(), null);
    }

    public static ApiResponse<Void> failure(final ErrorCode errorCode, final String overrideMessage) {
        final String message = Objects.requireNonNullElse(overrideMessage, errorCode.getDefaultMessage());
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    public static <T> ApiResponse<T> failure(final ErrorCode errorCode, final String overrideMessage, final T data) {
        final String message = Objects.requireNonNullElse(overrideMessage, errorCode.getDefaultMessage());
        return new ApiResponse<>(errorCode.getCode(), message, data);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
