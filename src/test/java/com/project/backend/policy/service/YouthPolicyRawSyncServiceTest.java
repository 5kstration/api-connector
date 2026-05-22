package com.project.backend.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.policy.client.YouthPolicyClient;
import com.project.backend.policy.dto.YouthPolicyParameter;
import com.project.backend.policy.dto.YouthPolicyRawSyncResultResponse;
import com.project.backend.raw.service.RawExternalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YouthPolicyRawSyncServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final YouthPolicyClient youthPolicyClient = mock(YouthPolicyClient.class);
    private final RawExternalService rawExternalService = mock(RawExternalService.class);
    private final YouthPolicyRawSyncService youthPolicyRawSyncService =
            new YouthPolicyRawSyncService(youthPolicyClient, rawExternalService, "/go/ythip/getPlcy");

    @Test
    @DisplayName("청년정책 목록은 빈 페이지가 나올 때까지 페이지 번호를 증가시키며 원본 저장한다")
    void syncRawCollectsYouthPoliciesUntilEmptyPage() throws Exception {
        JsonNode firstPage = objectMapper.readTree("""
                {
                  "youthPolicyList": [
                    { "plcyNo": "P001", "plcyNm": "첫 번째 정책" }
                  ]
                }
                """);
        JsonNode secondPage = objectMapper.readTree("""
                {
                  "youthPolicyList": [
                    { "plcyNo": "P002", "plcyNm": "두 번째 정책" }
                  ]
                }
                """);
        JsonNode emptyPage = objectMapper.readTree("""
                {
                  "youthPolicyList": []
                }
                """);

        when(youthPolicyClient.fetchPolicies(any(YouthPolicyParameter.class)))
                .thenReturn(firstPage)
                .thenReturn(secondPage)
                .thenReturn(emptyPage);
        when(youthPolicyClient.buildMaskedParams(any(YouthPolicyParameter.class)))
                .thenReturn(Map.of("apiKeyNm", "****", "pageNum", 1))
                .thenReturn(Map.of("apiKeyNm", "****", "pageNum", 2))
                .thenReturn(Map.of("apiKeyNm", "****", "pageNum", 3));

        YouthPolicyRawSyncResultResponse result = youthPolicyRawSyncService.syncRaw(null);

        assertThat(result.sourceCode()).isEqualTo("YOUTH_CENTER");
        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.failedCount()).isZero();

        ArgumentCaptor<YouthPolicyParameter> parameterCaptor = ArgumentCaptor.forClass(YouthPolicyParameter.class);
        verify(youthPolicyClient, times(3)).fetchPolicies(parameterCaptor.capture());

        assertThat(parameterCaptor.getAllValues())
                .extracting(YouthPolicyParameter::pageNum)
                .containsExactly(1, 2, 3);

        verify(rawExternalService, times(1)).saveIfAbsent(
                eq("YOUTH_CENTER"),
                eq("POLICY"),
                eq("/go/ythip/getPlcy"),
                any(),
                eq("P001"),
                any(JsonNode.class)
        );
        verify(rawExternalService, times(1)).saveIfAbsent(
                eq("YOUTH_CENTER"),
                eq("POLICY"),
                eq("/go/ythip/getPlcy"),
                any(),
                eq("P002"),
                any(JsonNode.class)
        );
    }

    @Test
    @DisplayName("중간 페이지에서 외부 API 호출이 실패하면 저장된 결과를 반환하고 동기화를 멈춘다")
    void syncRawStopsWithPartialResultWhenMiddlePageFails() throws Exception {
        JsonNode firstPage = objectMapper.readTree("""
                {
                  "youthPolicyList": [
                    { "plcyNo": "P001", "plcyNm": "첫 번째 정책" }
                  ]
                }
                """);

        when(youthPolicyClient.fetchPolicies(any(YouthPolicyParameter.class)))
                .thenReturn(firstPage)
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_CALL_FAILED));
        when(youthPolicyClient.buildMaskedParams(any(YouthPolicyParameter.class)))
                .thenReturn(Map.of("apiKeyNm", "****", "pageNum", 1));

        YouthPolicyRawSyncResultResponse result = youthPolicyRawSyncService.syncRaw(null);

        assertThat(result.requestedCount()).isEqualTo(1);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);

        verify(rawExternalService, times(1)).saveIfAbsent(
                eq("YOUTH_CENTER"),
                eq("POLICY"),
                eq("/go/ythip/getPlcy"),
                any(),
                eq("P001"),
                any(JsonNode.class)
        );
    }
}
