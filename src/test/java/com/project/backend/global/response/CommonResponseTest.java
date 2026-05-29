package com.project.backend.global.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonResponseTest {

    @Test
    @DisplayName("success(data)는 성공 응답을 반환한다")
    void successWithData() {
        CommonResponse<Integer> response = CommonResponse.success(1);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo(1);
        assertThat(response.message()).isNotBlank();
    }

    @Test
    @DisplayName("fail(message)는 실패 응답을 반환한다")
    void failWithMessage() {
        CommonResponse<Void> response = CommonResponse.fail("실패했습니다.");

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.message()).isEqualTo("실패했습니다.");
    }
}