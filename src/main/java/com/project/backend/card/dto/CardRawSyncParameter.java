package com.project.backend.card.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/*
 * 카드 크롤링 원본 적재 요청 파라미터입니다.
 */
public record CardRawSyncParameter(
        String sourceCode,
        @Min(1)
        @Max(100)
        Integer limit
) {
}
