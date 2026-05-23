package com.project.backend.insurance.client;

import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.insurance.dto.InsuranceRawSyncParameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsuranceApiClientTest {

    @Test
    @DisplayName("공공데이터 보험 API 요청 파라미터에서 serviceKey를 마스킹한다")
    void buildMaskedParamsMasksServiceKey() {
        InsuranceApiClient client = createClient("test-service-key");

        assertThat(client.buildMaskedParams(
                InsuranceApiClient.INDEMNITY_INSURANCE,
                parameter(null, null, null),
                1
        ))
                .containsEntry("serviceKey", "****")
                .containsEntry("resultType", "json")
                .containsEntry("pageNo", 1)
                .containsEntry("numOfRows", 100);
    }

    @Test
    @DisplayName("시민안전보험 API는 serviceKey 없이 지역 파라미터만 만든다")
    void buildMaskedParamsCreatesSafeInsuranceParamsWithoutServiceKey() {
        InsuranceApiClient client = createClient("");

        assertThat(client.buildMaskedParams(
                InsuranceApiClient.SAFE_INSURANCE,
                parameter(null, "6110000", "3220000"),
                1
        ))
                .doesNotContainKey("serviceKey")
                .containsEntry("upOrgCd", "6110000")
                .containsEntry("orgCd", "3220000")
                .containsEntry("mode", "L");
    }

    @Test
    @DisplayName("우체국 보험상품정보 조회에는 상품명이 필요하다")
    void postProductRequiresProductName() {
        InsuranceApiClient client = createClient("test-service-key");

        assertThatThrownBy(() -> client.buildMaskedParams(
                InsuranceApiClient.POST_INSURANCE_PRODUCT,
                parameter(null, null, null),
                1
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private InsuranceApiClient createClient(String serviceKey) {
        return new InsuranceApiClient(
                RestClient.builder(),
                serviceKey,
                "http://apis.data.go.kr",
                "/1160100/service/GetMedicalReimbursementInsuranceInfoService/getInsuranceInfo",
                "https://apis.data.go.kr",
                "/B552886/svc_postInsuBest/getPostInsuBestPrdt",
                "https://apis.data.go.kr",
                "/1721301/KrpostInsuranceProductView/InsuranceGoods",
                "https://apis.data.go.kr",
                "/1721301/KrpostInsuranceGuaranteeContentView/InsuranceGuranteeContent",
                "https://www.ins24.go.kr",
                "/api/safeInsrncInfoApi"
        );
    }

    private InsuranceRawSyncParameter parameter(String gdsNm, String upOrgCd, String orgCd) {
        return new InsuranceRawSyncParameter(
                null,
                1,
                100,
                1,
                "json",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                gdsNm,
                null,
                upOrgCd,
                orgCd,
                null,
                null,
                "L"
        );
    }
}
