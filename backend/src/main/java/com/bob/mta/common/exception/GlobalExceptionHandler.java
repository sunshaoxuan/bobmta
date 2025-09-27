package com.bob.mta.common.exception;

import com.bob.mta.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        HttpStatus status = errorCode.getStatus();
        ApiResponse<Object> body = ApiResponse.failure(errorCode.getCode(), exception.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials() {
        ErrorCode errorCode = ErrorCode.AUTHENTICATION_FAILED;
        ApiResponse<Object> body = ApiResponse.failure(errorCode.getCode(), errorCode.getDefaultMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied() {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        ApiResponse<Object> body = ApiResponse.failure(errorCode.getCode(), errorCode.getDefaultMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Object>> handleValidation(Exception exception) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        ApiResponse<Object> body = ApiResponse.failure(errorCode.getCode(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnknown(Exception exception) {
        ErrorCode errorCode = ErrorCode.UNKNOWN_ERROR;
        ApiResponse<Object> body = ApiResponse.failure(errorCode.getCode(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
