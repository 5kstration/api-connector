package com.project.backend.raw.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.raw.document.RawExternalDocument;
import com.project.backend.raw.repository.RawExternalRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@Service
public class RawExternalService {

    /*
     * 외부 API 응답 원본을 저장하는 서비스입니다.
     * 정제 데이터 저장보다 먼저 raw_externals에 원본을 남기는 역할을 합니다.
     */
    private final RawExternalRepository rawExternalRepository;
    private final ObjectMapper objectMapper;

    public RawExternalService(RawExternalRepository rawExternalRepository, ObjectMapper objectMapper) {
        this.rawExternalRepository = rawExternalRepository;
        this.objectMapper = objectMapper;
    }

    // sourceCode + rawHash 기준으로 중복이 없을 때만 원본을 저장합니다.
    public RawExternalDocument saveIfAbsent(
            String sourceCode,
            String category,
            String endpoint,
            Map<String, Object> requestParams,
            String externalId,
            JsonNode rawPayload
    ) {
        String rawHash = createRawHash(rawPayload);
        Map<String, Object> rawPayloadMap = convertToMap(rawPayload);
        return rawExternalRepository.findBySourceCodeAndRawHash(sourceCode, rawHash)
                .orElseGet(() -> rawExternalRepository.save(new RawExternalDocument(
                        sourceCode,
                        category,
                        endpoint,
                        requestParams,
                        externalId,
                        rawPayloadMap,
                        rawHash,
                        "SUCCESS"
                )));
    }

    // ExternalApiMetaDocument 1개에 연결된 RawExternalDocument 여러 건을 조회합니다.
    public List<RawExternalDocument> findBySourceCode(String sourceCode) {
        return rawExternalRepository.findAllBySourceCode(sourceCode);
    }

    private Map<String, Object> convertToMap(JsonNode rawPayload) {
        try {
            String json = objectMapper.writeValueAsString(rawPayload);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Raw payload cannot be converted to Map.", exception);
        }
    }

    // API마다 JSON 구조가 달라도 JsonNode 전체를 문자열화한 뒤 SHA-256 해시를 생성합니다.
    private String createRawHash(JsonNode rawPayload) {
        try {
            return sha256(objectMapper.writeValueAsString(rawPayload));
        } catch (JsonProcessingException exception) {
            return sha256(String.valueOf(rawPayload));
        }
    }

    // rawPayload 중복 여부를 안정적으로 비교하기 위한 SHA-256 해시 함수입니다.
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }
}
