package com.bob.mta.common.exception;

import com.bob.mta.common.api.ApiResponse;
<<<<<<< HEAD
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
=======
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts Java exceptions into unified REST responses while ensuring structured logging.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(final BusinessException ex) {
        final HttpStatus status = mapStatus(ex.getErrorCode());
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(ex.getErrorCode(), ex.getMessage()));
      
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(final BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationException(final MethodArgumentNotValidException ex) {
        final Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), Objects.toString(fieldError.getDefaultMessage(), "invalid"));
        }
        log.warn("Validation failed: {}", errors);
        return ApiResponse.failure(ErrorCode.BAD_REQUEST, "validation.failed", errors);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleRequestErrors(final Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ApiResponse.failure(ErrorCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpectedException(final Exception ex) {
        log.error("Unexpected error", ex);
        return ApiResponse.failure(ErrorCode.INTERNAL_ERROR);
    }

    private HttpStatus mapStatus(final ErrorCode errorCode) {
        return switch (errorCode) {
            case OK -> HttpStatus.OK;
            case BAD_REQUEST, ACTIVATION_TOKEN_INVALID, ACTIVATION_TOKEN_EXPIRED -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, USER_INACTIVE -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, USERNAME_EXISTS, EMAIL_EXISTS, USER_ALREADY_ACTIVE -> HttpStatus.CONFLICT;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
>>>>>>> origin/main
    }
}
