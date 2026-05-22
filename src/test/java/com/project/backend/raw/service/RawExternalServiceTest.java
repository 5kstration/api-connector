package com.project.backend.raw.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.raw.document.RawExternalDocument;
import com.project.backend.raw.repository.RawExternalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RawExternalServiceTest {

    @Mock
    private RawExternalRepository rawExternalRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RawExternalService rawExternalService;

    @BeforeEach
    void setUp() {
        rawExternalService = new RawExternalService(rawExternalRepository, objectMapper);
    }

    @Test
    @DisplayName("같은 sourceCode와 rawHash가 없으면 원본 데이터를 저장한다")
    void saveIfAbsentSavesNewRawExternal() throws Exception {
        JsonNode rawPayload = objectMapper.readTree("""
                {
                  "policyId": "P001",
                  "policyName": "청년 월세 지원"
                }
                """);
        when(rawExternalRepository.findBySourceCodeAndRawHash(any(), any())).thenReturn(Optional.empty());
        when(rawExternalRepository.save(any(RawExternalDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RawExternalDocument result = rawExternalService.saveIfAbsent(
                "YOUTH_CENTER",
                "POLICY",
                "/opi/youthPlcyList.do",
                Map.of("pageIndex", 1, "display", 100),
                "P001",
                rawPayload
        );

        ArgumentCaptor<RawExternalDocument> captor = ArgumentCaptor.forClass(RawExternalDocument.class);
        verify(rawExternalRepository).save(captor.capture());
        assertThat(result.getSourceCode()).isEqualTo("YOUTH_CENTER");
        assertThat(result.getRawHash()).isNotBlank();
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("같은 sourceCode와 rawHash가 있으면 기존 원본 데이터를 반환하고 저장하지 않는다")
    void saveIfAbsentReturnsExistingRawExternal() throws Exception {
        JsonNode rawPayload = objectMapper.readTree("""
                {
                  "productName": "시민안전보험",
                  "regionName": "서울"
                }
                """);
        RawExternalDocument existing = new RawExternalDocument(
                "SAFE_INSURANCE",
                "INSURANCE",
                "/api/safeInsrncInfoApi",
                Map.of("pageNo", 1),
                "SAFE-001",
                rawPayload,
                "already-exists",
                "SUCCESS"
        );
        when(rawExternalRepository.findBySourceCodeAndRawHash(any(), any())).thenReturn(Optional.of(existing));

        RawExternalDocument result = rawExternalService.saveIfAbsent(
                "SAFE_INSURANCE",
                "INSURANCE",
                "/api/safeInsrncInfoApi",
                Map.of("pageNo", 1),
                "SAFE-001",
                rawPayload
        );

        assertThat(result).isSameAs(existing);
        verify(rawExternalRepository, never()).save(any());
    }

    @Test
    @DisplayName("sourceCode에 연결된 원본 데이터 목록을 조회한다")
    void findBySourceCode() throws Exception {
        JsonNode rawPayload = objectMapper.readTree("{\"cardName\":\"sample\"}");
        RawExternalDocument rawExternal = new RawExternalDocument(
                "CARD_GORILLA",
                "CARD",
                "https://www.card-gorilla.com/search/card?cate=CRD",
                Map.of(),
                null,
                rawPayload,
                "hash",
                "SUCCESS"
        );
        when(rawExternalRepository.findAllBySourceCode("CARD_GORILLA")).thenReturn(List.of(rawExternal));

        List<RawExternalDocument> result = rawExternalService.findBySourceCode("CARD_GORILLA");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSourceCode()).isEqualTo("CARD_GORILLA");
    }
}