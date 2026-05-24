package com.project.backend.insurance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.insurance.client.InsuranceApiClient;
import com.project.backend.insurance.dto.InsuranceRawSyncParameter;
import com.project.backend.insurance.dto.InsuranceRawSyncResultResponse;
import com.project.backend.insurance.dto.SafeInsuranceRegion;
import com.project.backend.raw.service.RawExternalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InsuranceRawSyncServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InsuranceApiClient insuranceApiClient = mock(InsuranceApiClient.class);
    private final RawExternalService rawExternalService = mock(RawExternalService.class);
    private final SafeInsuranceRegionProvider safeInsuranceRegionProvider = mock(SafeInsuranceRegionProvider.class);
    private final InsuranceRawSyncService insuranceRawSyncService =
            new InsuranceRawSyncService(insuranceApiClient, rawExternalService, safeInsuranceRegionProvider);

    @Test
    @DisplayName("시민안전보험 응답 배열을 원본 데이터로 저장한다")
    void syncRawSavesSafeInsuranceArrayItems() throws Exception {
        JsonNode response = objectMapper.readTree("""
                [
                  {
                    "upOrgCd": "6110000",
                    "orgCd": "3220000",
                    "insrncGdsNm": "강남구 구민안전보험",
                    "grntFrom": "20240101",
                    "grntEnd": "20241231"
                  }
                ]
                """);
        InsuranceRawSyncParameter parameter = parameter(InsuranceApiClient.SAFE_INSURANCE);

        when(insuranceApiClient.fetch(InsuranceApiClient.SAFE_INSURANCE, parameter, 1)).thenReturn(response);
        when(insuranceApiClient.buildMaskedParams(InsuranceApiClient.SAFE_INSURANCE, parameter, 1))
                .thenReturn(Map.of("upOrgCd", "6110000", "orgCd", "3220000", "mode", "L"));
        when(insuranceApiClient.endpoint(InsuranceApiClient.SAFE_INSURANCE))
                .thenReturn("/api/safeInsrncInfoApi");

        InsuranceRawSyncResultResponse result = insuranceRawSyncService.syncRaw(parameter);

        assertThat(result.requestedCount()).isEqualTo(1);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        assertThat(result.sourceResults()).hasSize(1);

        verify(rawExternalService, times(1)).saveIfAbsent(
                eq(InsuranceApiClient.SAFE_INSURANCE),
                eq("INSURANCE"),
                eq("/api/safeInsrncInfoApi"),
                any(),
                eq("6110000|3220000|강남구 구민안전보험|20240101|20241231"),
                any(JsonNode.class)
        );
    }

    @Test
    @DisplayName("실손보험 items.item 객체를 원본 데이터로 저장한다")
    void syncRawSavesIndemnityItemObject() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "body": {
                    "items": {
                      "item": {
                        "basDt": "20240501",
                        "cmpyCd": "001",
                        "prdNm": "실손보험",
                        "age": "25",
                        "ptrn": "표준형",
                        "mog": "상해"
                      }
                    },
                    "numOfRows": 100,
                    "pageNo": 1,
                    "totalCount": 1
                  }
                }
                """);
        InsuranceRawSyncParameter parameter = parameter(InsuranceApiClient.INDEMNITY_INSURANCE);

        when(insuranceApiClient.fetch(InsuranceApiClient.INDEMNITY_INSURANCE, parameter, 1)).thenReturn(response);
        when(insuranceApiClient.buildMaskedParams(InsuranceApiClient.INDEMNITY_INSURANCE, parameter, 1))
                .thenReturn(Map.of("serviceKey", "****", "pageNo", 1, "numOfRows", 100));
        when(insuranceApiClient.endpoint(InsuranceApiClient.INDEMNITY_INSURANCE))
                .thenReturn("/1160100/service/GetMedicalReimbursementInsuranceInfoService/getInsuranceInfo");

        InsuranceRawSyncResultResponse result = insuranceRawSyncService.syncRaw(parameter);

        assertThat(result.requestedCount()).isEqualTo(1);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();

        verify(rawExternalService, times(1)).saveIfAbsent(
                eq(InsuranceApiClient.INDEMNITY_INSURANCE),
                eq("INSURANCE"),
                eq("/1160100/service/GetMedicalReimbursementInsuranceInfoService/getInsuranceInfo"),
                any(),
                eq("001|실손보험|25|20240501|표준형|상해"),
                any(JsonNode.class)
        );
    }

    @Test
    @DisplayName("기본 보험 자동 적재는 JSON 지역 목록 기준으로 시민안전보험을 호출한다")
    void syncRawUsesSafeInsuranceRegionsFromJsonProviderWhenSourceCodeIsEmpty() throws Exception {
        JsonNode emptyPageResponse = objectMapper.readTree("""
                {
                  "body": {
                    "items": {
                      "item": []
                    },
                    "numOfRows": 100,
                    "pageNo": 1,
                    "totalCount": 0
                  }
                }
                """);
        JsonNode safeInsuranceResponse = objectMapper.readTree("""
                [
                  {
                    "upOrgCd": "6110000",
                    "orgCd": "3220000",
                    "insrncGdsNm": "강남구 구민안전보험",
                    "grntFrom": "20240101",
                    "grntEnd": "20241231"
                  }
                ]
                """);

        when(insuranceApiClient.fetch(eq(InsuranceApiClient.INDEMNITY_INSURANCE), any(), eq(1)))
                .thenReturn(emptyPageResponse);
        when(insuranceApiClient.fetch(eq(InsuranceApiClient.POST_INSURANCE_BEST), any(), eq(1)))
                .thenReturn(emptyPageResponse);
        when(insuranceApiClient.fetch(eq(InsuranceApiClient.SAFE_INSURANCE), any(), eq(1)))
                .thenReturn(safeInsuranceResponse);
        when(insuranceApiClient.buildMaskedParams(any(), any(), eq(1)))
                .thenReturn(Map.of());
        when(insuranceApiClient.endpoint(any()))
                .thenReturn("/api/test");
        when(safeInsuranceRegionProvider.findEnabledRegions())
                .thenReturn(List.of(
                        new SafeInsuranceRegion("6110000", "서울특별시", "3000000", "종로구", true),
                        new SafeInsuranceRegion("6110000", "서울특별시", "3220000", "강남구", true)
                ));

        insuranceRawSyncService.syncRaw(null);

        ArgumentCaptor<InsuranceRawSyncParameter> parameterCaptor =
                ArgumentCaptor.forClass(InsuranceRawSyncParameter.class);
        verify(insuranceApiClient, times(2)).fetch(
                eq(InsuranceApiClient.SAFE_INSURANCE),
                parameterCaptor.capture(),
                eq(1)
        );

        assertThat(parameterCaptor.getAllValues())
                .extracting(InsuranceRawSyncParameter::orgCd)
                .containsExactly("3000000", "3220000");
        assertThat(parameterCaptor.getAllValues())
                .extracting(InsuranceRawSyncParameter::upOrgCd)
                .containsExactly("6110000", "6110000");
        assertThat(parameterCaptor.getAllValues())
                .extracting(InsuranceRawSyncParameter::mode)
                .containsExactly("L", "L");
    }

    @Test
    @DisplayName("기본 보험 자동 적재는 실손보험을 청년 대표 연령으로 제한해 호출한다")
    void syncRawUsesDefaultYouthAgesForIndemnityInsurance() throws Exception {
        JsonNode emptyPageResponse = objectMapper.readTree("""
                {
                  "body": {
                    "items": {
                      "item": []
                    },
                    "numOfRows": 100,
                    "pageNo": 1,
                    "totalCount": 0
                  }
                }
                """);
        JsonNode safeInsuranceResponse = objectMapper.readTree("""
                []
                """);

        when(insuranceApiClient.fetch(eq(InsuranceApiClient.INDEMNITY_INSURANCE), any(), eq(1)))
                .thenReturn(emptyPageResponse);
        when(insuranceApiClient.fetch(eq(InsuranceApiClient.POST_INSURANCE_BEST), any(), eq(1)))
                .thenReturn(emptyPageResponse);
        when(insuranceApiClient.fetch(eq(InsuranceApiClient.SAFE_INSURANCE), any(), eq(1)))
                .thenReturn(safeInsuranceResponse);
        when(insuranceApiClient.buildMaskedParams(any(), any(), eq(1)))
                .thenReturn(Map.of());
        when(insuranceApiClient.endpoint(any()))
                .thenReturn("/api/test");
        when(safeInsuranceRegionProvider.findEnabledRegions())
                .thenReturn(List.of(new SafeInsuranceRegion("6110000", "서울특별시", "3220000", "강남구", true)));

        insuranceRawSyncService.syncRaw(null);

        ArgumentCaptor<InsuranceRawSyncParameter> parameterCaptor =
                ArgumentCaptor.forClass(InsuranceRawSyncParameter.class);
        verify(insuranceApiClient, times(4)).fetch(
                eq(InsuranceApiClient.INDEMNITY_INSURANCE),
                parameterCaptor.capture(),
                eq(1)
        );

        assertThat(parameterCaptor.getAllValues())
                .extracting(InsuranceRawSyncParameter::age)
                .containsExactly("20", "25", "30", "34");
    }

    private InsuranceRawSyncParameter parameter(String sourceCode) {
        return new InsuranceRawSyncParameter(
                sourceCode,
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
                null,
                null,
                "6110000",
                "3220000",
                null,
                null,
                "L"
        );
    }
}
