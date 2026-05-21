package com.moneylog.apiconnector.global.exception;

/*
exception 예외처리
BusinessException으로 errocode 메세지를 던짐
GlobalExceptionHandler 의 ExceptionHandler 로 캐치
ErrCode의 상태코드 반환
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
