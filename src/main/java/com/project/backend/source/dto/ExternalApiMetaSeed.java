package com.project.backend.source.dto;

/*
 * src/main/resources/seed/external-api-metas.json을 읽기 위한 seed DTO입니다.
 * API Key나 serviceKey 같은 민감한 인증값은 이 파일에 넣지 않습니다.
 */
public record ExternalApiMetaSeed(
        String sourceCode,
        String sourceName,
        String sourceType,
        String category,
        String baseUrl,
        String authType,
        boolean enabled,
        String syncCycle
) {
}
