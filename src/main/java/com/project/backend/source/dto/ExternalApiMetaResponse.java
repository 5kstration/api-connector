package com.project.backend.source.dto;

import com.project.backend.source.document.ExternalApiMetaDocument;

import java.time.LocalDateTime;

public record ExternalApiMetaResponse(
        String id,
        String sourceCode,
        String sourceName,
        String sourceType,
        String category,
        String baseUrl,
        String authType,
        boolean enabled,
        String syncCycle,
        LocalDateTime lastSyncedAt
) {

    public static ExternalApiMetaResponse from(ExternalApiMetaDocument document) {
        return new ExternalApiMetaResponse(
                document.getId(),
                document.getSourceCode(),
                document.getSourceName(),
                document.getSourceType(),
                document.getCategory(),
                document.getBaseUrl(),
                document.getAuthType(),
                document.isEnabled(),
                document.getSyncCycle(),
                document.getLastSyncedAt()
        );
    }
}
