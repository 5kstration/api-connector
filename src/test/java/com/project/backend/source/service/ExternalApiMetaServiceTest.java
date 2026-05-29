package com.project.backend.source.service;

import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.source.document.ExternalApiMetaDocument;
import com.project.backend.source.dto.ExternalApiMetaResponse;
import com.project.backend.source.repository.ExternalApiMetaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalApiMetaServiceTest {

    @Mock
    private ExternalApiMetaRepository externalApiMetaRepository;

    @InjectMocks
    private ExternalApiMetaService externalApiMetaService;

    @Test
    @DisplayName("활성화된 외부 API 메타데이터를 응답 DTO로 변환해 반환한다")
    void findEnabledSources() {
        ExternalApiMetaDocument source = new ExternalApiMetaDocument(
                "YOUTH_CENTER",
                "온통청년 청년정책 API",
                "OPEN_API",
                "POLICY",
                "https://www.youthcenter.go.kr",
                "API_KEY",
                true,
                "DAILY"
        );
        when(externalApiMetaRepository.findAllByEnabledTrue()).thenReturn(List.of(source));

        List<ExternalApiMetaResponse> responses = externalApiMetaService.findEnabledSources();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).sourceCode()).isEqualTo("YOUTH_CENTER");
        assertThat(responses.get(0).category()).isEqualTo("POLICY");
    }

    @Test
    @DisplayName("sourceCode로 외부 API 메타데이터를 조회한다")
    void getBySourceCode() {
        ExternalApiMetaDocument source = new ExternalApiMetaDocument(
                "SAFE_INSURANCE",
                "시민안전보험 Open API",
                "OPEN_API",
                "INSURANCE",
                "https://www.ins24.go.kr",
                "NONE",
                true,
                "DAILY"
        );
        when(externalApiMetaRepository.findBySourceCode("SAFE_INSURANCE")).thenReturn(Optional.of(source));

        ExternalApiMetaDocument result = externalApiMetaService.getBySourceCode("SAFE_INSURANCE");

        assertThat(result.getSourceCode()).isEqualTo("SAFE_INSURANCE");
        assertThat(result.getCategory()).isEqualTo("INSURANCE");
    }

    @Test
    @DisplayName("sourceCode에 해당하는 메타데이터가 없으면 BusinessException을 던진다")
    void getBySourceCodeNotFound() {
        when(externalApiMetaRepository.findBySourceCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> externalApiMetaService.getBySourceCode("UNKNOWN"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("동기화 성공 시 마지막 동기화 시각을 갱신하고 저장한다")
    void updateLastSyncedAt() {
        ExternalApiMetaDocument source = new ExternalApiMetaDocument(
                "YOUTH_CENTER",
                "온통청년 청년정책 API",
                "OPEN_API",
                "POLICY",
                "https://www.youthcenter.go.kr",
                "API_KEY",
                true,
                "DAILY"
        );
        LocalDateTime syncedAt = LocalDateTime.of(2026, 5, 21, 3, 0);
        when(externalApiMetaRepository.findBySourceCode("YOUTH_CENTER")).thenReturn(Optional.of(source));

        externalApiMetaService.updateLastSyncedAt("YOUTH_CENTER", syncedAt);

        assertThat(source.getLastSyncedAt()).isEqualTo(syncedAt);
        verify(externalApiMetaRepository).save(source);
    }
}