package com.project.backend.insurance.dto;

import java.time.LocalDateTime;
import java.util.List;

/*
 * 보험 API 원본 데이터 적재 결과입니다.
 */
public record InsuranceRawSyncResultResponse(
        int requestedCount,
        int processedCount,
        int failedCount,
        List<SourceResult> sourceResults,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {

    public record SourceResult(
            String sourceCode,
            int requestedCount,
            int processedCount,
            int failedCount
    ) {
    }
}
