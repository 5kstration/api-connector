package com.project.backend.raw.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/*
 * raw_externals 목록 조회 조건입니다.
 * sourceCode, category, externalId, status 기준으로 원본 데이터를 필터링합니다.
 */
public record RawExternalSearchCondition(
        String sourceCode,
        String category,
        String externalId,
        String status,
        @Min(0)
        Integer page,
        @Min(1)
        @Max(100)
        Integer size
) {

    public RawExternalSearchCondition {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
