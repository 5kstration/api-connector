package com.project.backend.policy.client;

import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.policy.dto.YouthPolicyParameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YouthPolicyClientTest {

    @Test
    @DisplayName("기본 pageSize가 1보다 작거나 100보다 크면 클라이언트 생성에 실패한다")
    void constructorFailsWhenDefaultPageSizeIsOutOfRange() {
        assertThatThrownBy(() -> createClient(0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("external-api.youth-center.page-size는 1~100 범위여야 합니다.");

        assertThatThrownBy(() -> createClient(101))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("external-api.youth-center.page-size는 1~100 범위여야 합니다.");
    }

    @Test
    @DisplayName("pageNum이 1보다 작으면 잘못된 요청으로 실패한다")
    void buildParamsFailsWhenPageNumIsLessThanOne() {
        YouthPolicyClient client = createClient(100);
        YouthPolicyParameter parameter = new YouthPolicyParameter(
                0,
                100,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> client.buildParams(parameter))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("pageSize가 1보다 작거나 100보다 크면 잘못된 요청으로 실패한다")
    void buildParamsFailsWhenPageSizeIsOutOfRange() {
        YouthPolicyClient client = createClient(100);
        YouthPolicyParameter tooSmallPageSize = parameterWithPageSize(0);
        YouthPolicyParameter tooLargePageSize = parameterWithPageSize(101);

        assertThatThrownBy(() -> client.buildParams(tooSmallPageSize))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        assertThatThrownBy(() -> client.buildParams(tooLargePageSize))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("정상 범위의 페이지 요청 파라미터를 생성한다")
    void buildParamsCreatesValidPageParams() {
        YouthPolicyClient client = createClient(100);

        assertThat(client.buildParams(parameterWithPageSize(50)))
                .containsEntry("pageNum", 1)
                .containsEntry("pageSize", 50)
                .containsEntry("pageType", "1")
                .containsEntry("rtnType", "json");
    }

    private YouthPolicyClient createClient(int defaultPageSize) {
        return new YouthPolicyClient(
                RestClient.builder(),
                "https://www.youthcenter.go.kr",
                "/go/ythip/getPlcy",
                "test-api-key",
                defaultPageSize,
                "json",
                "1"
        );
    }

    private YouthPolicyParameter parameterWithPageSize(int pageSize) {
        return new YouthPolicyParameter(
                1,
                pageSize,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
