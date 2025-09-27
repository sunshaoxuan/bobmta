package com.bob.mta.common.api;

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
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

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
}
