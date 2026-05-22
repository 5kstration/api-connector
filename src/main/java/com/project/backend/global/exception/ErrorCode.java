package com.project.backend.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    EXTERNAL_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다."),
    EXTERNAL_API_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, "외부 API 응답이 비어 있습니다."),
    CRAWLING_FAILED(HttpStatus.BAD_GATEWAY, "크롤링에 실패했습니다."),
    DATA_NORMALIZE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "외부 데이터 정제에 실패했습니다."),
    SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "외부 데이터 소스를 찾을 수 없습니다."),
    SYNC_ALREADY_RUNNING(HttpStatus.CONFLICT, "동기화 작업이 이미 실행 중입니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
