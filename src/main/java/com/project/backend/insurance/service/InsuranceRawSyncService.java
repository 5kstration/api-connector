package com.project.backend.insurance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.insurance.client.InsuranceApiClient;
import com.project.backend.insurance.dto.InsuranceRawSyncParameter;
import com.project.backend.insurance.dto.InsuranceRawSyncResultResponse;
import com.project.backend.raw.service.RawExternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InsuranceRawSyncService {

    private static final Logger log = LoggerFactory.getLogger(InsuranceRawSyncService.class);

    private static final String CATEGORY = "INSURANCE";
    private static final int DEFAULT_START_PAGE = 1;
    private static final int DEFAULT_MAX_PAGE = 1;
    private static final int MAX_DERIVED_PRODUCT_NAMES = 10;
    private static final List<String> DEFAULT_YOUTH_AGES = List.of("20", "25", "30", "34");
    private static final String DEFAULT_SAFE_UP_ORG_CD = "6110000";
    private static final String DEFAULT_SAFE_ORG_CD = "3220000";
    private static final String DEFAULT_SAFE_MODE = "L";

    private final InsuranceApiClient insuranceApiClient;
    private final RawExternalService rawExternalService;

    public InsuranceRawSyncService(
            InsuranceApiClient insuranceApiClient,
            RawExternalService rawExternalService
    ) {
        this.insuranceApiClient = insuranceApiClient;
        this.rawExternalService = rawExternalService;
    }

    /*
     * 보험 API 응답 원본을 raw_externals에 저장합니다.
     * sourceCode가 없으면 실손보험, 우체국 베스트, 시민안전보험을 기본 수집하고
     * 베스트 상품명을 이용해 우체국 상품정보와 보장내용을 추가 조회합니다.
     */
    public InsuranceRawSyncResultResponse syncRaw(InsuranceRawSyncParameter parameter) {
        LocalDateTime startedAt = LocalDateTime.now();
        List<InsuranceRawSyncResultResponse.SourceResult> sourceResults = new ArrayList<>();

        if (parameter != null && StringUtils.hasText(parameter.sourceCode())) {
            sourceResults.add(syncBySource(parameter.sourceCode(), parameter));
        } else {
            for (String age : DEFAULT_YOUTH_AGES) {
                sourceResults.add(syncPageSource(
                        InsuranceApiClient.INDEMNITY_INSURANCE,
                        withIndemnityAge(parameter, age)
                ));
            }
            SourceSyncResult postBestResult = syncPostBestAndCollectProductNames(parameter);
            sourceResults.add(postBestResult.sourceResult());

            for (String productName : limitedProductNames(postBestResult.productNames())) {
                sourceResults.add(syncBySource(
                        InsuranceApiClient.POST_INSURANCE_PRODUCT,
                        withProductName(parameter, productName)
                ));
                sourceResults.add(syncBySource(
                        InsuranceApiClient.POST_INSURANCE_COVERAGE,
                        withCoverageName(parameter, productName)
                ));
            }

            sourceResults.add(syncBySource(
                    InsuranceApiClient.SAFE_INSURANCE,
                    withDefaultSafeInsuranceRegion(parameter)
            ));
        }

        int requestedCount = sourceResults.stream().mapToInt(InsuranceRawSyncResultResponse.SourceResult::requestedCount).sum();
        int processedCount = sourceResults.stream().mapToInt(InsuranceRawSyncResultResponse.SourceResult::processedCount).sum();
        int failedCount = sourceResults.stream().mapToInt(InsuranceRawSyncResultResponse.SourceResult::failedCount).sum();

        return new InsuranceRawSyncResultResponse(
                requestedCount,
                processedCount,
                failedCount,
                sourceResults,
                startedAt,
                LocalDateTime.now()
        );
    }

    private InsuranceRawSyncResultResponse.SourceResult syncBySource(
            String sourceCode,
            InsuranceRawSyncParameter parameter
    ) {
        return switch (sourceCode) {
            case InsuranceApiClient.INDEMNITY_INSURANCE,
                 InsuranceApiClient.POST_INSURANCE_BEST,
                 InsuranceApiClient.POST_INSURANCE_COVERAGE -> syncPageSource(sourceCode, parameter);
            case InsuranceApiClient.POST_INSURANCE_PRODUCT,
                 InsuranceApiClient.SAFE_INSURANCE -> syncSingleSource(sourceCode, parameter);
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 보험 sourceCode입니다.");
        };
    }

    private SourceSyncResult syncPostBestAndCollectProductNames(InsuranceRawSyncParameter parameter) {
        int requestedCount = 0;
        int processedCount = 0;
        int failedCount = 0;
        Set<String> productNames = new LinkedHashSet<>();

        int pageNo = startPage(parameter);
        int maxPage = maxPage(parameter);

        for (int i = 0; i < maxPage; i++) {
            PageSyncResult pageResult = syncPage(InsuranceApiClient.POST_INSURANCE_BEST, parameter, pageNo);

            requestedCount += pageResult.requestedCount();
            processedCount += pageResult.processedCount();
            failedCount += pageResult.failedCount();
            productNames.addAll(extractProductNames(pageResult.items()));

            if (pageResult.requestedCount() == 0 || !hasNextPage(pageResult.response(), pageNo)) {
                break;
            }
            pageNo++;
            sleepBeforeNextCall();
        }

        return new SourceSyncResult(
                new InsuranceRawSyncResultResponse.SourceResult(
                        InsuranceApiClient.POST_INSURANCE_BEST,
                        requestedCount,
                        processedCount,
                        failedCount
                ),
                productNames
        );
    }

    private InsuranceRawSyncResultResponse.SourceResult syncPageSource(
            String sourceCode,
            InsuranceRawSyncParameter parameter
    ) {
        int requestedCount = 0;
        int processedCount = 0;
        int failedCount = 0;

        int pageNo = startPage(parameter);
        int maxPage = maxPage(parameter);

        for (int i = 0; i < maxPage; i++) {
            PageSyncResult pageResult = syncPage(sourceCode, parameter, pageNo);

            requestedCount += pageResult.requestedCount();
            processedCount += pageResult.processedCount();
            failedCount += pageResult.failedCount();

            if (pageResult.requestedCount() == 0 || !hasNextPage(pageResult.response(), pageNo)) {
                break;
            }
            pageNo++;
            sleepBeforeNextCall();
        }

        return new InsuranceRawSyncResultResponse.SourceResult(
                sourceCode,
                requestedCount,
                processedCount,
                failedCount
        );
    }

    private InsuranceRawSyncResultResponse.SourceResult syncSingleSource(
            String sourceCode,
            InsuranceRawSyncParameter parameter
    ) {
        PageSyncResult pageResult = syncPage(sourceCode, parameter, startPage(parameter));
        return new InsuranceRawSyncResultResponse.SourceResult(
                sourceCode,
                pageResult.requestedCount(),
                pageResult.processedCount(),
                pageResult.failedCount()
        );
    }

    private PageSyncResult syncPage(String sourceCode, InsuranceRawSyncParameter parameter, int pageNo) {
        JsonNode response = insuranceApiClient.fetch(sourceCode, parameter, pageNo);
        List<JsonNode> items = extractItems(response);
        Map<String, Object> requestParams = insuranceApiClient.buildMaskedParams(sourceCode, parameter, pageNo);
        String endpoint = insuranceApiClient.endpoint(sourceCode);

        int processedCount = 0;
        int failedCount = 0;

        for (JsonNode item : items) {
            String externalId = externalId(sourceCode, item);
            try {
                rawExternalService.saveIfAbsent(
                        sourceCode,
                        CATEGORY,
                        endpoint,
                        requestParams,
                        externalId,
                        item
                );
                processedCount++;
            } catch (RuntimeException exception) {
                log.warn("Failed to save insurance item: sourceCode={}, externalId={}", sourceCode, externalId, exception);
                failedCount++;
            }
        }

        return new PageSyncResult(response, items, items.size(), processedCount, failedCount);
    }

    private List<JsonNode> extractItems(JsonNode response) {
        if (response == null || response.isNull()) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_RESPONSE_EMPTY);
        }

        if (response.isArray()) {
            return toList(response);
        }

        JsonNode itemNode = findNode(response, "item");
        if (itemNode != null) {
            if (itemNode.isArray()) {
                return toList(itemNode);
            }
            if (itemNode.isObject()) {
                return List.of(itemNode);
            }
        }

        if (response.isObject() && hasInsuranceDataField(response)) {
            return List.of(response);
        }
        return List.of();
    }

    private boolean hasNextPage(JsonNode response, int currentPageNo) {
        int totalCount = intValue(findNode(response, "totalCount"));
        int numOfRows = intValue(findNode(response, "numOfRows"));

        if (totalCount <= 0 || numOfRows <= 0) {
            return false;
        }
        return currentPageNo * numOfRows < totalCount;
    }

    private Set<String> extractProductNames(List<JsonNode> items) {
        Set<String> productNames = new LinkedHashSet<>();
        for (JsonNode item : items) {
            String productName = firstText(item, "INSU_GDS_NM", "GDS_NM", "prdNm", "INSU_NM");
            if (StringUtils.hasText(productName)) {
                productNames.add(productName);
            }
        }
        return productNames;
    }

    private List<String> limitedProductNames(Set<String> productNames) {
        return productNames.stream()
                .limit(MAX_DERIVED_PRODUCT_NAMES)
                .toList();
    }

    private String externalId(String sourceCode, JsonNode item) {
        return switch (sourceCode) {
            case InsuranceApiClient.INDEMNITY_INSURANCE -> join(
                    text(item, "cmpyCd"),
                    text(item, "prdNm"),
                    text(item, "age"),
                    text(item, "basDt"),
                    text(item, "ptrn"),
                    text(item, "mog")
            );
            case InsuranceApiClient.POST_INSURANCE_BEST -> join(
                    text(item, "CRTR_YM"),
                    text(item, "INSU_GDS_NM")
            );
            case InsuranceApiClient.POST_INSURANCE_PRODUCT -> text(item, "GDS_NM");
            case InsuranceApiClient.POST_INSURANCE_COVERAGE -> join(
                    text(item, "INSU_NM"),
                    text(item, "SNTC_LINE_NO"),
                    text(item, "APLCN_BGNG_YMD")
            );
            case InsuranceApiClient.SAFE_INSURANCE -> join(
                    text(item, "upOrgCd"),
                    text(item, "orgCd"),
                    text(item, "insrncGdsNm"),
                    text(item, "grntFrom"),
                    text(item, "grntEnd")
            );
            default -> null;
        };
    }

    private InsuranceRawSyncParameter withProductName(InsuranceRawSyncParameter parameter, String productName) {
        return new InsuranceRawSyncParameter(
                InsuranceApiClient.POST_INSURANCE_PRODUCT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                productName,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private InsuranceRawSyncParameter withIndemnityAge(InsuranceRawSyncParameter parameter, String age) {
        return new InsuranceRawSyncParameter(
                InsuranceApiClient.INDEMNITY_INSURANCE,
                parameter == null ? null : parameter.pageNo(),
                parameter == null ? null : parameter.numOfRows(),
                parameter == null ? null : parameter.maxPage(),
                textOrDefault(parameter == null ? null : parameter.resultType(), "json"),
                parameter == null ? null : parameter.basDt(),
                parameter == null ? null : parameter.beginBasDt(),
                parameter == null ? null : parameter.endBasDt(),
                parameter == null ? null : parameter.likeBasDt(),
                parameter == null ? null : parameter.cmpyCd(),
                parameter == null ? null : parameter.cmpyNm(),
                parameter == null ? null : parameter.likeCmpyNm(),
                parameter == null ? null : parameter.ptrn(),
                parameter == null ? null : parameter.mog(),
                parameter == null ? null : parameter.prdNm(),
                parameter == null ? null : parameter.likePrdNm(),
                textOrDefault(parameter == null ? null : parameter.age(), age),
                parameter == null ? null : parameter.ofrInstNm(),
                parameter == null ? null : parameter.gdsNm(),
                parameter == null ? null : parameter.insuNm(),
                parameter == null ? null : parameter.upOrgCd(),
                parameter == null ? null : parameter.orgCd(),
                parameter == null ? null : parameter.mdfcnBgngYmd(),
                parameter == null ? null : parameter.mdfcnEndYmd(),
                parameter == null ? null : parameter.mode()
        );
    }

    private InsuranceRawSyncParameter withCoverageName(InsuranceRawSyncParameter parameter, String insuranceName) {
        return new InsuranceRawSyncParameter(
                InsuranceApiClient.POST_INSURANCE_COVERAGE,
                parameter == null ? null : parameter.pageNo(),
                parameter == null ? null : parameter.numOfRows(),
                parameter == null ? null : parameter.maxPage(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                insuranceName,
                null,
                null,
                null,
                null,
                null
        );
    }

    private InsuranceRawSyncParameter withDefaultSafeInsuranceRegion(InsuranceRawSyncParameter parameter) {
        return new InsuranceRawSyncParameter(
                InsuranceApiClient.SAFE_INSURANCE,
                parameter == null ? null : parameter.pageNo(),
                parameter == null ? null : parameter.numOfRows(),
                parameter == null ? null : parameter.maxPage(),
                parameter == null ? null : parameter.resultType(),
                parameter == null ? null : parameter.basDt(),
                parameter == null ? null : parameter.beginBasDt(),
                parameter == null ? null : parameter.endBasDt(),
                parameter == null ? null : parameter.likeBasDt(),
                parameter == null ? null : parameter.cmpyCd(),
                parameter == null ? null : parameter.cmpyNm(),
                parameter == null ? null : parameter.likeCmpyNm(),
                parameter == null ? null : parameter.ptrn(),
                parameter == null ? null : parameter.mog(),
                parameter == null ? null : parameter.prdNm(),
                parameter == null ? null : parameter.likePrdNm(),
                parameter == null ? null : parameter.age(),
                parameter == null ? null : parameter.ofrInstNm(),
                parameter == null ? null : parameter.gdsNm(),
                parameter == null ? null : parameter.insuNm(),
                textOrDefault(parameter == null ? null : parameter.upOrgCd(), DEFAULT_SAFE_UP_ORG_CD),
                textOrDefault(parameter == null ? null : parameter.orgCd(), DEFAULT_SAFE_ORG_CD),
                parameter == null ? null : parameter.mdfcnBgngYmd(),
                parameter == null ? null : parameter.mdfcnEndYmd(),
                textOrDefault(parameter == null ? null : parameter.mode(), DEFAULT_SAFE_MODE)
        );
    }

    private List<JsonNode> toList(JsonNode arrayNode) {
        List<JsonNode> result = new ArrayList<>();
        arrayNode.forEach(result::add);
        return result;
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

    private boolean hasInsuranceDataField(JsonNode node) {
        return node.has("cmpyNm")
                || node.has("INSU_GDS_NM")
                || node.has("GDS_NM")
                || node.has("INSU_NM")
                || node.has("insrncGdsNm");
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = text(node, fieldName);
            if (StringUtils.hasText(value)) {
                return value;
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

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String join(String... values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                result.add(value);
            }
        }
        return result.isEmpty() ? null : String.join("|", result);
    }

    private int startPage(InsuranceRawSyncParameter parameter) {
        if (parameter == null || parameter.pageNo() == null) {
            return DEFAULT_START_PAGE;
        }
        return parameter.pageNo();
    }

    private int maxPage(InsuranceRawSyncParameter parameter) {
        if (parameter == null || parameter.maxPage() == null) {
            return DEFAULT_MAX_PAGE;
        }
        return parameter.maxPage();
    }

    private int intValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        String text = node.asText();
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void sleepBeforeNextCall() {
        try {
            Thread.sleep(300L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.EXTERNAL_API_CALL_FAILED, "보험 API 페이지 수집이 중단되었습니다.");
        }
    }

    private record PageSyncResult(
            JsonNode response,
            List<JsonNode> items,
            int requestedCount,
            int processedCount,
            int failedCount
    ) {
    }

    private record SourceSyncResult(
            InsuranceRawSyncResultResponse.SourceResult sourceResult,
            Set<String> productNames
    ) {
    }
}
