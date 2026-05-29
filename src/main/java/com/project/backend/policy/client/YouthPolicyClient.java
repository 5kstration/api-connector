package com.project.backend.policy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.policy.dto.YouthPolicyParameter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class YouthPolicyClient {

    /*
     * 온통청년 청년정책 API 호출을 담당합니다.
     * 응답은 정제하지 않고 JsonNode 그대로 반환하여 RawExternalDocument.rawPayload에 저장할 수 있게 합니다.
     */
    private final RestClient restClient;
    private final String policyPath;
    private final String apiKey;
    private final int defaultPageSize;
    private final String defaultRtnType;
    private final String defaultListPageType;

    public YouthPolicyClient(
            RestClient.Builder restClientBuilder,
            @Value("${external-api.youth-center.base-url}") String baseUrl,
            @Value("${external-api.youth-center.policy-path}") String policyPath,
            @Value("${external-api.youth-center.api-key}") String apiKey,
            @Value("${external-api.youth-center.page-size}") int defaultPageSize,
            @Value("${external-api.youth-center.rtn-type}") String defaultRtnType,
            @Value("${external-api.youth-center.list-page-type}") String defaultListPageType
    ) {
        if (defaultPageSize < 1 || defaultPageSize > 100) {
            throw new IllegalStateException("external-api.youth-center.page-size는 1~100 범위여야 합니다.");
        }
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.policyPath = policyPath;
        this.apiKey = apiKey;
        this.defaultPageSize = defaultPageSize;
        this.defaultRtnType = defaultRtnType;
        this.defaultListPageType = defaultListPageType;
    }

    public JsonNode fetchPolicies(YouthPolicyParameter parameter) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "청년정책 API 키가 설정되어 있지 않습니다.");
        }

        Map<String, Object> params = buildParams(parameter);
        try {
            return restClient.get()
                    .uri(uriBuilder -> buildUri(uriBuilder, params))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_CALL_FAILED, "청년정책 API 호출에 실패했습니다.");
        }
    }

    public Map<String, Object> buildParams(YouthPolicyParameter parameter) {
        int pageNum = parameter != null && parameter.pageNum() != null ? parameter.pageNum() : 1;
        int pageSize = parameter != null && parameter.pageSize() != null ? parameter.pageSize() : defaultPageSize;
        if (pageNum < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "pageNum은 1 이상이어야 합니다.");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "pageSize는 1~100 범위여야 합니다.");
        }
        String pageType = parameter != null && StringUtils.hasText(parameter.pageType())
                ? parameter.pageType()
                : defaultListPageType;
        String rtnType = parameter != null && StringUtils.hasText(parameter.rtnType())
                ? parameter.rtnType()
                : defaultRtnType;

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("apiKeyNm", apiKey);
        params.put("pageNum", pageNum);
        params.put("pageSize", pageSize);
        params.put("pageType", pageType);
        params.put("rtnType", rtnType);

        if (parameter != null) {
            putIfHasText(params, "plcyNo", parameter.policyNo());
            putIfHasText(params, "plcyKywdNm", parameter.keywordName());
            putIfHasText(params, "plcyExplnCn", parameter.policyDescription());
            putIfHasText(params, "plcyNm", parameter.policyName());
            putIfHasText(params, "zipCd", parameter.regionCode());
            putIfHasText(params, "lclsfNm", parameter.largeCategoryName());
            putIfHasText(params, "mclsfNm", parameter.middleCategoryName());
        }
        return params;
    }

    public Map<String, Object> buildMaskedParams(YouthPolicyParameter parameter) {
        Map<String, Object> params = new LinkedHashMap<>(buildParams(parameter));
        params.put("apiKeyNm", "****");
        return params;
    }

    private URI buildUri(UriBuilder uriBuilder, Map<String, Object> params) {
        UriBuilder builder = uriBuilder.path(policyPath);
        params.forEach(builder::queryParam);
        return builder.build();
    }

    private void putIfHasText(Map<String, Object> params, String key, String value) {
        if (StringUtils.hasText(value)) {
            params.put(key, value);
        }
    }
}
