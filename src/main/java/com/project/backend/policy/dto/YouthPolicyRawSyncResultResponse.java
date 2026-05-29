package com.project.backend.policy.dto;

import java.time.LocalDateTime;

/*
 * 청년정책 원본 데이터 적재 결과를 표현합니다.
 * 정제 데이터 결과가 아니라 raw_externals 저장 흐름의 실행 결과입니다.
 */
public record YouthPolicyRawSyncResultResponse(
        String sourceCode,
        int requestedCount,
        int processedCount,
        int failedCount,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
