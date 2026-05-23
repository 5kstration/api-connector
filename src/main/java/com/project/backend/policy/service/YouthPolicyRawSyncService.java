package com.project.backend.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.policy.client.YouthPolicyClient;
import com.project.backend.policy.dto.YouthPolicyParameter;
import com.project.backend.policy.dto.YouthPolicyRawSyncResultResponse;
import com.project.backend.raw.service.RawExternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class YouthPolicyRawSyncService {

    private static final Logger log = LoggerFactory.getLogger(YouthPolicyRawSyncService.class);

    private static final String SOURCE_CODE = "YOUTH_CENTER";
    private static final String CATEGORY = "POLICY";
    private static final int DEFAULT_START_PAGE = 1;
    private static final int MAX_PAGE_COUNT = 1000;

    private final YouthPolicyClient youthPolicyClient;
    private final RawExternalService rawExternalService;
    private final String policyPath;

    public YouthPolicyRawSyncService(
            YouthPolicyClient youthPolicyClient,
            RawExternalService rawExternalService,
            @Value("${external-api.youth-center.policy-path}") String policyPath
    ) {
        this.youthPolicyClient = youthPolicyClient;
        this.rawExternalService = rawExternalService;
        this.policyPath = policyPath;
    }

    /*
     * 온통청년 API를 호출하고 응답 원본을 raw_externals에 저장합니다.
     * 목록 조회는 pageNum을 증가시키며 빈 페이지가 나올 때까지 수집합니다.
     */
    public YouthPolicyRawSyncResultResponse syncRaw(YouthPolicyParameter parameter) {
        LocalDateTime startedAt = LocalDateTime.now();
        int requestedCount = 0;
        int processedCount = 0;
        int failedCount = 0;

        if (isSingleRequest(parameter)) {
            SyncPageResult result = syncPage(parameter);
            requestedCount += result.requestedCount();
            processedCount += result.processedCount();
            failedCount += result.failedCount();
        } else {
            int pageNum = startPage(parameter);
            int pageCount = 0;

            while (pageCount < MAX_PAGE_COUNT) {
                YouthPolicyParameter pageParameter = withPageNum(parameter, pageNum);
                SyncPageResult result;

                try {
                    result = syncPage(pageParameter);
                } catch (BusinessException exception) {
                    if (requestedCount == 0 && processedCount == 0) {
                        throw exception;
                    }
                    failedCount++;
                    break;
                }

                if (result.requestedCount() == 0) {
                    break;
                }

                requestedCount += result.requestedCount();
                processedCount += result.processedCount();
                failedCount += result.failedCount();

                pageNum++;
                pageCount++;
                sleepBeforeNextPage();
            }
        }

        return new YouthPolicyRawSyncResultResponse(
                SOURCE_CODE,
                requestedCount,
                processedCount,
                failedCount,
                startedAt,
                LocalDateTime.now()
        );
    }

    private SyncPageResult syncPage(YouthPolicyParameter parameter) {
        JsonNode response = youthPolicyClient.fetchPolicies(parameter);
        List<JsonNode> policyItems = extractPolicyItems(response);
        Map<String, Object> requestParams = youthPolicyClient.buildMaskedParams(parameter);

        int processedCount = 0;
        int failedCount = 0;

        for (JsonNode policyItem : policyItems) {
            String externalId = text(policyItem, "plcyNo");
            try {
                rawExternalService.saveIfAbsent(
                        SOURCE_CODE,
                        CATEGORY,
                        policyPath,
                        requestParams,
                        externalId,
                        policyItem
                );
                processedCount++;
            } catch (RuntimeException exception) {
                log.warn("Failed to save policy item: externalId={}", externalId, exception);
                failedCount++;
            }
        }

        return new SyncPageResult(policyItems.size(), processedCount, failedCount);
    }

    /*
     * 온통청년 JSON 응답에서 정책 목록 배열을 찾습니다.
     * 응답 구조가 조금 달라져도 youthPolicyList 필드를 우선 탐색합니다.
     */
    private List<JsonNode> extractPolicyItems(JsonNode response) {
        if (response == null || response.isNull()) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_RESPONSE_EMPTY);
        }

        JsonNode policyListNode = findNode(response, "youthPolicyList");
        if (policyListNode == null) {
            policyListNode = findNode(response, "policyList");
        }
        if (policyListNode == null) {
            policyListNode = findNode(response, "data");
        }
        if (policyListNode == null) {
            policyListNode = response;
        }

        List<JsonNode> result = new ArrayList<>();
        if (policyListNode.isArray()) {
            policyListNode.forEach(result::add);
            return result;
        }
        if (policyListNode.isObject() && policyListNode.has("plcyNo")) {
            result.add(policyListNode);
        }
        return result;
    }

    private boolean isSingleRequest(YouthPolicyParameter parameter) {
        return parameter != null && StringUtils.hasText(parameter.policyNo());
    }

    private int startPage(YouthPolicyParameter parameter) {
        if (parameter == null || parameter.pageNum() == null) {
            return DEFAULT_START_PAGE;
        }
        return parameter.pageNum();
    }

    private YouthPolicyParameter withPageNum(YouthPolicyParameter parameter, int pageNum) {
        if (parameter == null) {
            return new YouthPolicyParameter(
                    pageNum,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        return new YouthPolicyParameter(
                pageNum,
                parameter.pageSize(),
                parameter.pageType(),
                parameter.rtnType(),
                parameter.policyNo(),
                parameter.keywordName(),
                parameter.policyDescription(),
                parameter.policyName(),
                parameter.regionCode(),
                parameter.largeCategoryName(),
                parameter.middleCategoryName()
        );
    }

    private JsonNode findNode(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.has(fieldName)) {
            return node.get(fieldName);
        }
        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findNode(child, fieldName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private void sleepBeforeNextPage() {
        try {
            Thread.sleep(300L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.EXTERNAL_API_CALL_FAILED, "청년정책 API 페이지 수집이 중단되었습니다.");
        }
    }

    private record SyncPageResult(
            int requestedCount,
            int processedCount,
            int failedCount
    ) {
    }
}
