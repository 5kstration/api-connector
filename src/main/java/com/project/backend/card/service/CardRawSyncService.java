package com.project.backend.card.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.card.crawler.TossCardLoungeCrawler;
import com.project.backend.card.dto.CardRawSyncParameter;
import com.project.backend.card.dto.CardRawSyncResultResponse;
import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.raw.service.RawExternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CardRawSyncService {

    private static final Logger log = LoggerFactory.getLogger(CardRawSyncService.class);

    private final TossCardLoungeCrawler tossCardLoungeCrawler;
    private final RawExternalService rawExternalService;
    private final ObjectMapper objectMapper;

    public CardRawSyncService(
            TossCardLoungeCrawler tossCardLoungeCrawler,
            RawExternalService rawExternalService,
            ObjectMapper objectMapper
    ) {
        this.tossCardLoungeCrawler = tossCardLoungeCrawler;
        this.rawExternalService = rawExternalService;
        this.objectMapper = objectMapper;
    }

    /*
     * 카드 크롤링 결과를 raw_externals에 저장합니다.
     * 현재 MVP에서는 토스 카드라운지를 우선 지원합니다.
     */
    public CardRawSyncResultResponse syncRaw(CardRawSyncParameter parameter) {
        LocalDateTime startedAt = LocalDateTime.now();
        String sourceCode = sourceCode(parameter);
        int processedCount = 0;
        int failedCount = 0;

        if (!TossCardLoungeCrawler.SOURCE_CODE.equals(sourceCode)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 카드 sourceCode입니다.");
        }

        var cards = tossCardLoungeCrawler.crawl(parameter == null ? null : parameter.limit());
        for (TossCardLoungeCrawler.TossCardPayload card : cards) {
            try {
                JsonNode rawPayload = objectMapper.valueToTree(card.toRawPayload());
                rawExternalService.saveIfAbsent(
                        TossCardLoungeCrawler.SOURCE_CODE,
                        tossCardLoungeCrawler.category(),
                        tossCardLoungeCrawler.listUrl(),
                        Map.of("limit", parameter == null || parameter.limit() == null ? "" : parameter.limit()),
                        card.externalId(),
                        rawPayload
                );
                processedCount++;
            } catch (RuntimeException exception) {
                log.warn("Failed to save card item: sourceCode={}, externalId={}",
                        TossCardLoungeCrawler.SOURCE_CODE,
                        card.externalId(),
                        exception);
                failedCount++;
            }
        }

        return new CardRawSyncResultResponse(
                TossCardLoungeCrawler.SOURCE_CODE,
                cards.size(),
                processedCount,
                failedCount,
                startedAt,
                LocalDateTime.now()
        );
    }

    private String sourceCode(CardRawSyncParameter parameter) {
        if (parameter == null || !StringUtils.hasText(parameter.sourceCode())) {
            return TossCardLoungeCrawler.SOURCE_CODE;
        }
        return parameter.sourceCode();
    }
}
