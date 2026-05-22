package com.project.backend.source.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/*
 * 외부 API 또는 크롤링 출처의 메타데이터 Document
 *
 * - YOUTH_CENTER: 온통청년 청년정책 API
 * - CARD_GORILLA: 카드고릴라 크롤링
 * - SAFE_INSURANCE: 시민안전보험 Open API
 *
 * RawExternalDocument는 sourceCode 1:N
 */
@Document(collection = "external_api_metas")
public class ExternalApiMetaDocument {

    @Id
    private String id;                          // MongoDB ObjectId

    @Indexed(unique = true)
    private String sourceCode;                  // 외부 출처 코드

    private String sourceName;                  // 사람이 읽기 쉬운 API 이름
    private String sourceType;                  // 수집 방식
    private String category;                    // 데이터 분류 POLICY, CARD, INSURANCE
    private String baseUrl;                     // 외부 API 또는 크롤링 대상 기본 URL
    private String authType;                    // 인증 방식 NONE, API_KEY, SERVICE_KEY
    private boolean enabled;                    // 현재 동기화 대상으로 사용할지 여부
    private String syncCycle;                   // 동기화 주기 cron
    private LocalDateTime lastSyncedAt;         // 마지막 동기화
    private LocalDateTime createdAt;            // 생성 시각
    private LocalDateTime updatedAt;            // 수정 시각

    protected ExternalApiMetaDocument() {
    }

    public ExternalApiMetaDocument(
            String sourceCode,
            String sourceName,
            String sourceType,
            String category,
            String baseUrl,
            String authType,
            boolean enabled,
            String syncCycle
    ) {
        LocalDateTime now = LocalDateTime.now();
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.category = category;
        this.baseUrl = baseUrl;
        this.authType = authType;
        this.enabled = enabled;
        this.syncCycle = syncCycle;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getCategory() {
        return category;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSyncCycle() {
        return syncCycle;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
        this.updatedAt = LocalDateTime.now();
    }
}
