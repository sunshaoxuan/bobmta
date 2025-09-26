package com.bob.mta.common.api;

<<<<<<< HEAD
import java.time.OffsetDateTime;
import java.util.Objects;

public class ApiResponse<T> {

    private final OffsetDateTime timestamp;
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(Builder<T> builder) {
        this.timestamp = builder.timestamp;
        this.success = builder.success;
        this.code = builder.code;
        this.message = builder.message;
        this.data = builder.data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("OK")
                .message("success")
                .data(data)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
=======
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
>>>>>>> origin/main
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
<<<<<<< HEAD

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public Builder<T> toBuilder() {
        return new Builder<T>()
                .timestamp(timestamp)
                .success(success)
                .code(code)
                .message(message)
                .data(data);
    }

    public static final class Builder<T> {
        private OffsetDateTime timestamp;
        private boolean success;
        private String code;
        private String message;
        private T data;

        private Builder() {
        }

        public Builder<T> timestamp(OffsetDateTime timestamp) {
            this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
            return this;
        }

        public Builder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder<T> code(String code) {
            this.code = code;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public ApiResponse<T> build() {
            return new ApiResponse<>(this);
        }
    }
=======
>>>>>>> origin/main
}
