package com.project.backend.insurance.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.insurance.dto.InsuranceRawSyncParameter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InsuranceApiClient {

    public static final String INDEMNITY_INSURANCE = "INDEMNITY_INSURANCE";
    public static final String POST_INSURANCE_BEST = "POST_INSURANCE_BEST";
    public static final String POST_INSURANCE_PRODUCT = "POST_INSURANCE_PRODUCT";
    public static final String POST_INSURANCE_COVERAGE = "POST_INSURANCE_COVERAGE";
    public static final String SAFE_INSURANCE = "SAFE_INSURANCE";

    private final RestClient.Builder restClientBuilder;
    private final String dataGoKrServiceKey;
    private final InsuranceApiSpec indemnitySpec;
    private final InsuranceApiSpec postBestSpec;
    private final InsuranceApiSpec postProductSpec;
    private final InsuranceApiSpec postCoverageSpec;
    private final InsuranceApiSpec safeInsuranceSpec;

    public InsuranceApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${external-api.insurance.data-go-kr-service-key}") String dataGoKrServiceKey,
            @Value("${external-api.insurance.indemnity.base-url}") String indemnityBaseUrl,
            @Value("${external-api.insurance.indemnity.path}") String indemnityPath,
            @Value("${external-api.insurance.post-best.base-url}") String postBestBaseUrl,
            @Value("${external-api.insurance.post-best.path}") String postBestPath,
            @Value("${external-api.insurance.post-product.base-url}") String postProductBaseUrl,
            @Value("${external-api.insurance.post-product.path}") String postProductPath,
            @Value("${external-api.insurance.post-coverage.base-url}") String postCoverageBaseUrl,
            @Value("${external-api.insurance.post-coverage.path}") String postCoveragePath,
            @Value("${external-api.insurance.safe-insurance.base-url}") String safeInsuranceBaseUrl,
            @Value("${external-api.insurance.safe-insurance.path}") String safeInsurancePath
    ) {
        this.restClientBuilder = restClientBuilder;
        this.dataGoKrServiceKey = dataGoKrServiceKey;
        this.indemnitySpec = new InsuranceApiSpec(indemnityBaseUrl, indemnityPath, true);
        this.postBestSpec = new InsuranceApiSpec(postBestBaseUrl, postBestPath, true);
        this.postProductSpec = new InsuranceApiSpec(postProductBaseUrl, postProductPath, true);
        this.postCoverageSpec = new InsuranceApiSpec(postCoverageBaseUrl, postCoveragePath, true);
        this.safeInsuranceSpec = new InsuranceApiSpec(safeInsuranceBaseUrl, safeInsurancePath, false);
    }

    public JsonNode fetch(String sourceCode, InsuranceRawSyncParameter parameter, int pageNo) {
        InsuranceApiSpec spec = spec(sourceCode);
        Map<String, Object> params = buildParams(sourceCode, parameter, pageNo);

        try {
            return restClientBuilder.baseUrl(spec.baseUrl())
                    .build()
                    .get()
                    .uri(uriBuilder -> buildUri(uriBuilder, spec.path(), params))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_CALL_FAILED, "보험 API 호출에 실패했습니다.");
        }
    }

    public Map<String, Object> buildMaskedParams(String sourceCode, InsuranceRawSyncParameter parameter, int pageNo) {
        Map<String, Object> params = new LinkedHashMap<>(buildParams(sourceCode, parameter, pageNo));
        if (params.containsKey("serviceKey")) {
            params.put("serviceKey", "****");
        }
        return params;
    }

    public String endpoint(String sourceCode) {
        return spec(sourceCode).path();
    }

    public boolean requiresServiceKey(String sourceCode) {
        return spec(sourceCode).requiresServiceKey();
    }

    private Map<String, Object> buildParams(String sourceCode, InsuranceRawSyncParameter parameter, int pageNo) {
        validatePage(pageNo, numOfRows(parameter));

        Map<String, Object> params = new LinkedHashMap<>();
        if (requiresServiceKey(sourceCode)) {
            if (!StringUtils.hasText(dataGoKrServiceKey)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "공공데이터포털 serviceKey가 설정되어 있지 않습니다.");
            }
            params.put("serviceKey", dataGoKrServiceKey);
        }

        switch (sourceCode) {
            case INDEMNITY_INSURANCE -> putIndemnityParams(params, parameter, pageNo);
            case POST_INSURANCE_BEST -> putPostBestParams(params, parameter, pageNo);
            case POST_INSURANCE_PRODUCT -> putPostProductParams(params, parameter);
            case POST_INSURANCE_COVERAGE -> putPostCoverageParams(params, parameter, pageNo);
            case SAFE_INSURANCE -> putSafeInsuranceParams(params, parameter);
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 보험 sourceCode입니다.");
        }

        return params;
    }

    private void putIndemnityParams(Map<String, Object> params, InsuranceRawSyncParameter parameter, int pageNo) {
        params.put("resultType", textOrDefault(parameter == null ? null : parameter.resultType(), "json"));
        params.put("pageNo", pageNo);
        params.put("numOfRows", numOfRows(parameter));
        if (parameter != null) {
            putIfHasText(params, "basDt", parameter.basDt());
            putIfHasText(params, "beginBasDt", parameter.beginBasDt());
            putIfHasText(params, "endBasDt", parameter.endBasDt());
            putIfHasText(params, "likeBasDt", parameter.likeBasDt());
            putIfHasText(params, "cmpyCd", parameter.cmpyCd());
            putIfHasText(params, "cmpyNm", parameter.cmpyNm());
            putIfHasText(params, "likeCmpyNm", parameter.likeCmpyNm());
            putIfHasText(params, "ptrn", parameter.ptrn());
            putIfHasText(params, "mog", parameter.mog());
            putIfHasText(params, "prdNm", parameter.prdNm());
            putIfHasText(params, "likePrdNm", parameter.likePrdNm());
            putIfHasText(params, "age", parameter.age());
            putIfHasText(params, "ofrInstNm", parameter.ofrInstNm());
        }
    }

    private void putPostBestParams(Map<String, Object> params, InsuranceRawSyncParameter parameter, int pageNo) {
        params.put("pageNo", pageNo);
        params.put("numOfRows", numOfRows(parameter));
    }

    private void putPostProductParams(Map<String, Object> params, InsuranceRawSyncParameter parameter) {
        if (parameter == null || !StringUtils.hasText(parameter.gdsNm())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "우체국 보험상품정보 조회에는 gdsNm이 필요합니다.");
        }
        params.put("GDS_NM", parameter.gdsNm());
    }

    private void putPostCoverageParams(Map<String, Object> params, InsuranceRawSyncParameter parameter, int pageNo) {
        if (parameter == null || !StringUtils.hasText(parameter.insuNm())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "우체국보험 보장내용 조회에는 insuNm이 필요합니다.");
        }
        params.put("pageNo", pageNo);
        params.put("numOfRows", numOfRows(parameter));
        params.put("INSU_NM", parameter.insuNm());
    }

    private void putSafeInsuranceParams(Map<String, Object> params, InsuranceRawSyncParameter parameter) {
        if (parameter == null) {
            return;
        }
        putIfHasText(params, "upOrgCd", parameter.upOrgCd());
        putIfHasText(params, "orgCd", parameter.orgCd());
        putIfHasText(params, "mdfcnBgngYmd", parameter.mdfcnBgngYmd());
        putIfHasText(params, "mdfcnEndYmd", parameter.mdfcnEndYmd());
        putIfHasText(params, "mode", parameter.mode());
    }

    private InsuranceApiSpec spec(String sourceCode) {
        return switch (sourceCode) {
            case INDEMNITY_INSURANCE -> indemnitySpec;
            case POST_INSURANCE_BEST -> postBestSpec;
            case POST_INSURANCE_PRODUCT -> postProductSpec;
            case POST_INSURANCE_COVERAGE -> postCoverageSpec;
            case SAFE_INSURANCE -> safeInsuranceSpec;
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 보험 sourceCode입니다.");
        };
    }

    private URI buildUri(UriBuilder uriBuilder, String path, Map<String, Object> params) {
        UriBuilder builder = uriBuilder.path(path);
        params.forEach(builder::queryParam);
        return builder.build();
    }

    private int numOfRows(InsuranceRawSyncParameter parameter) {
        return parameter != null && parameter.numOfRows() != null ? parameter.numOfRows() : 100;
    }

    private void validatePage(int pageNo, int numOfRows) {
        if (pageNo < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "pageNo는 1 이상이어야 합니다.");
        }
        if (numOfRows < 1 || numOfRows > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "numOfRows는 1~100 범위여야 합니다.");
        }
    }

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private void putIfHasText(Map<String, Object> params, String key, String value) {
        if (StringUtils.hasText(value)) {
            params.put(key, value);
        }
    }

    private record InsuranceApiSpec(
            String baseUrl,
            String path,
            boolean requiresServiceKey
    ) {
    }
}
