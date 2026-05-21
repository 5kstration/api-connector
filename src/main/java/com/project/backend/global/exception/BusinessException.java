package com.project.backend.global.exception;

/*
 * 서비스 계층에서 의도적으로 던지는 비즈니스 예외입니다.
 * ErrorCode를 함께 보관해 GlobalExceptionHandler가 상태 코드와 메시지를 결정할 수 있게 합니다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
