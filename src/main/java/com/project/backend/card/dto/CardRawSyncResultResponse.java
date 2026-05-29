package com.project.backend.card.dto;

import java.time.LocalDateTime;

/*
 * 카드 크롤링 원본 데이터 적재 결과입니다.
 */
public record CardRawSyncResultResponse(
        String sourceCode,
        int requestedCount,
        int processedCount,
        int failedCount,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
