package com.project.backend.source.service;

import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.source.document.ExternalApiMetaDocument;
import com.project.backend.source.dto.ExternalApiMetaResponse;
import com.project.backend.source.repository.ExternalApiMetaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExternalApiMetaService {

    /*
     * 외부 API 메타데이터 조회와 동기화 시각 갱신을 담당
     * 실제 외부 API 호출은 policy/card/insurance 쪽 SyncService에서 처리
     */
    private final ExternalApiMetaRepository externalApiMetaRepository;

    public ExternalApiMetaService(ExternalApiMetaRepository externalApiMetaRepository) {
        this.externalApiMetaRepository = externalApiMetaRepository;
    }

    // enabled=true 외부 출처 목록 응답 DTO로 변환 반환
    public List<ExternalApiMetaResponse> findEnabledSources() {
        return externalApiMetaRepository.findAllByEnabledTrue()
                .stream()
                .map(ExternalApiMetaResponse::from)
                .toList();
    }

    // sourceCode 기준으로 외부 출처 메타데이터 조회
    public ExternalApiMetaDocument getBySourceCode(String sourceCode) {
        return externalApiMetaRepository.findBySourceCode(sourceCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOURCE_NOT_FOUND));
    }

    // 동기화 후 동기화 시각을 갱신
    public void updateLastSyncedAt(String sourceCode, LocalDateTime syncedAt) {
        ExternalApiMetaDocument source = getBySourceCode(sourceCode);
        source.updateLastSyncedAt(syncedAt);
        externalApiMetaRepository.save(source);
    }
}
