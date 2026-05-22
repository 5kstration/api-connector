package com.project.backend.raw.document;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/*
 * 외부 API 응답 또는 크롤링 결과를 정제하기 전 원본으로 저장하는 Document입니다.
 *
 * API마다 JSON 필드가 모두 다를 수 있으므로 rawPayload는 JsonNode로 저장합니다.
 * 예: 온통청년 정책 JSON, 시민안전보험 JSON, 카드 크롤링 결과 JSON이 모두 다른 구조여도 저장 가능
 *
 * sourceCode는 ExternalApiMetaDocument.sourceCode와 논리적인 1:N 관계를 가집니다.
 */
@Document(collection = "raw_externals")
@CompoundIndex(name = "source_raw_hash_idx", def = "{'sourceCode': 1, 'rawHash': 1}", unique = true)
public class RawExternalDocument {

    @Id
    private String id;                          // MongoDB ObjectId

    @Indexed
    private String sourceCode;                  // 원본 데이터가 속한 외부 출처 코드

    private String category;                    // POLICY, CARD, INSURANCE
    private String endpoint;                    // 호출한 API path 또는 크롤링 URL
    private Map<String, Object> requestParams;  // 외부 API 호출에 사용한 요청 파라미터
    private String externalId;                  // 외부 API가 제공하는 고유 ID. 없으면 null 가능
    private JsonNode rawPayload;                // 외부 응답 원본 JSON

    @Indexed
    private String rawHash;                     // 원본 중복 저장 방지를 위한 SHA-256 해시

    private LocalDateTime fetchedAt;            // 원본 데이터를 수집한 시각
    private String status;                      // 수집 상태. 예: SUCCESS, FAILED

    protected RawExternalDocument() {
    }

    public RawExternalDocument(
            String sourceCode,
            String category,
            String endpoint,
            Map<String, Object> requestParams,
            String externalId,
            JsonNode rawPayload,
            String rawHash,
            String status
    ) {
        this.sourceCode = sourceCode;
        this.category = category;
        this.endpoint = endpoint;
        this.requestParams = requestParams;
        this.externalId = externalId;
        this.rawPayload = rawPayload;
        this.rawHash = rawHash;
        this.fetchedAt = LocalDateTime.now();
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getCategory() {
        return category;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Map<String, Object> getRequestParams() {
        return requestParams;
    }

    public String getExternalId() {
        return externalId;
    }

    public JsonNode getRawPayload() {
        return rawPayload;
    }

    public String getRawHash() {
        return rawHash;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public String getStatus() {
        return status;
    }
}
