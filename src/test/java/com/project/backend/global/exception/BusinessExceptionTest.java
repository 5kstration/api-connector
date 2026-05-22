package com.project.backend.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    @DisplayName("BusinessException은 ErrorCode와 기본 메시지를 보관한다")
    void businessExceptionContainsErrorCode() {
        BusinessException exception = new BusinessException(ErrorCode.SOURCE_NOT_FOUND);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOURCE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.SOURCE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("BusinessException은 사용자 정의 메시지를 사용할 수 있다")
    void businessExceptionWithCustomMessage() {
        BusinessException exception = new BusinessException(
                ErrorCode.EXTERNAL_API_CALL_FAILED,
                "온통청년 API 호출 실패"
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_CALL_FAILED);
        assertThat(exception.getMessage()).isEqualTo("온통청년 API 호출 실패");
    }
}