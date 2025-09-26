package com.bob.mta.common.exception;

<<<<<<< HEAD
=======
/**
 * Exception representing domain/business rule violations.
 */
>>>>>>> origin/main
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

<<<<<<< HEAD
    public BusinessException(ErrorCode errorCode) {
=======
    public BusinessException(final ErrorCode errorCode) {
>>>>>>> origin/main
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

<<<<<<< HEAD
    public BusinessException(ErrorCode errorCode, String message) {
=======
    public BusinessException(final ErrorCode errorCode, final String message) {
>>>>>>> origin/main
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
