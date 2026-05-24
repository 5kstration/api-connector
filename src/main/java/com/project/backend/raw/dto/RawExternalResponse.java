package com.project.backend.raw.dto;

import com.project.backend.raw.document.RawExternalDocument;

import java.time.LocalDateTime;
import java.util.Map;

/*
 * raw_externals 조회 응답 DTO입니다.
 * Controller에서 MongoDB Document를 직접 반환하지 않기 위해 사용합니다.
 */
public record RawExternalResponse(
        String id,
        String sourceCode,
        String category,
        String endpoint,
        Map<String, Object> requestParams,
        String externalId,
        Map<String, Object> rawPayload,
        String rawHash,
        LocalDateTime fetchedAt,
        String status
) {

    public static RawExternalResponse from(RawExternalDocument document) {
        return new RawExternalResponse(
                document.getId(),
                document.getSourceCode(),
                document.getCategory(),
                document.getEndpoint(),
                document.getRequestParams(),
                document.getExternalId(),
                document.getRawPayload(),
                document.getRawHash(),
                document.getFetchedAt(),
                document.getStatus()
        );
    }
}
