package com.project.backend.card.crawler;

import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TossCardLoungeCrawler {

    private static final Logger log = LoggerFactory.getLogger(TossCardLoungeCrawler.class);

    public static final String SOURCE_CODE = "TOSS_CARD_LOUNGE";
    private static final String CATEGORY = "CARD";
    private static final String BASE_URL = "https://card-lounge.toss.im";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    private final String listUrl;

    public TossCardLoungeCrawler(
            @Value("${external-api.card.toss-card-lounge.url}") String listUrl
    ) {
        this.listUrl = listUrl;
    }

    public List<TossCardPayload> crawl(Integer limit) {
        Document listDocument = fetch(listUrl);
        List<TossCardSummary> summaries = parseList(listDocument, limit);
        List<TossCardPayload> result = new ArrayList<>();

        for (TossCardSummary summary : summaries) {
            TossCardDetail detail = fetchDetailSafely(summary);

            result.add(new TossCardPayload(
                    summary.cardId(),
                    summary.cardName(),
                    summary.summary(),
                    summary.imageUrl(),
                    summary.detailUrl(),
                    detail.annualFeeText(),
                    detail.previousMonthRequirement(),
                    detail.benefitTexts(),
                    detail.rawDetailText()
            ));
            sleepBeforeNextCall();
        }
        return result;
    }

    private TossCardDetail fetchDetailSafely(TossCardSummary summary) {
        if (!StringUtils.hasText(summary.detailUrl())) {
            return TossCardDetail.empty();
        }
        try {
            return parseDetail(fetch(summary.detailUrl()));
        } catch (RuntimeException exception) {
            log.warn("Failed to crawl Toss card detail: cardId={}, cardName={}, detailUrl={}",
                    summary.cardId(),
                    summary.cardName(),
                    summary.detailUrl(),
                    exception);
            return TossCardDetail.empty();
        }
    }

    public List<TossCardSummary> parseList(Document document, Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        Elements links = document.select("a[href^=/card/]");
        List<TossCardSummary> result = new ArrayList<>();

        for (Element link : links) {
            if (result.size() >= normalizedLimit) {
                break;
            }

            String detailPath = link.attr("href");
            String cardId = extractCardId(detailPath);
            String detailUrl = BASE_URL + detailPath;
            String cardName = text(link.selectFirst("[data-desktop-list-item-title]"));
            if (!StringUtils.hasText(cardName)) {
                cardName = text(link.selectFirst("img[alt]"));
            }
            if (!StringUtils.hasText(cardName)) {
                continue;
            }

            String imageUrl = attr(link.selectFirst("img[src]"), "abs:src");
            String summary = summaryText(link, cardName);

            result.add(new TossCardSummary(
                    cardId,
                    cardName,
                    summary,
                    imageUrl,
                    detailUrl
            ));
        }
        return result;
    }

    public TossCardDetail parseDetail(Document document) {
        String rawText = normalizeText(document.body().text());
        String annualFeeText = textBetween(rawText, "연회비", "전월실적");
        String previousMonthRequirement = textBetween(rawText, "전월실적", "혜택 안내");
        List<String> benefitTexts = benefitTexts(document);

        return new TossCardDetail(
                annualFeeText,
                previousMonthRequirement,
                benefitTexts,
                rawText
        );
    }

    public String listUrl() {
        return listUrl;
    }

    public String category() {
        return CATEGORY;
    }

    private Document fetch(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10_000)
                    .get();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.CRAWLING_FAILED, "토스 카드라운지 크롤링에 실패했습니다.");
        }
    }

    private List<String> benefitTexts(Document document) {
        Element benefitHeader = document.selectFirst("*:matchesOwn(^혜택 안내$)");
        Element benefitSection = nearestSection(benefitHeader);
        Elements buttons = benefitSection == null
                ? document.select("button:contains(자세히 보기)")
                : benefitSection.select("button");

        List<String> result = new ArrayList<>();
        for (Element button : buttons) {
            String text = benefitText(button);
            if (StringUtils.hasText(text) && !result.contains(text)) {
                result.add(text);
            }
        }
        return result;
    }

    private String benefitText(Element button) {
        List<String> textParts = new ArrayList<>();
        for (Element child : button.children()) {
            String text = normalizeText(child.text());
            if (StringUtils.hasText(text) && !"자세히 보기".equals(text)) {
                textParts.add(text);
            }
        }
        String text = textParts.isEmpty() ? button.text() : String.join(" / ", textParts);
        text = text.replace("자세히 보기", "");
        return normalizeText(text);
    }

    private Element nearestSection(Element element) {
        Element current = element;
        for (int i = 0; i < 8 && current != null; i++) {
            if ("section".equalsIgnoreCase(current.tagName())) {
                return current;
            }
            current = current.parent();
        }
        return null;
    }

    private String summaryText(Element link, String cardName) {
        String fullText = normalizeText(link.text());
        String summary = fullText.replace(cardName, "").trim();
        summary = summary.replaceFirst("^\\d+\\s+", "");
        return StringUtils.hasText(summary) ? summary : null;
    }

    private String extractCardId(String detailPath) {
        if (!StringUtils.hasText(detailPath)) {
            return null;
        }
        String prefix = "/card/";
        int index = detailPath.indexOf(prefix);
        if (index < 0) {
            return null;
        }
        String value = detailPath.substring(index + prefix.length());
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        return StringUtils.hasText(value) ? value : null;
    }

    private String textBetween(String text, String start, String end) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        int startIndex = text.indexOf(start);
        if (startIndex < 0) {
            return null;
        }
        int valueStart = startIndex + start.length();
        int endIndex = text.indexOf(end, valueStart);
        String value = endIndex < 0 ? text.substring(valueStart) : text.substring(valueStart, endIndex);
        value = normalizeText(value);
        return StringUtils.hasText(value) ? value : null;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private String text(Element element) {
        if (element == null) {
            return null;
        }
        String text = element.text();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String attr(Element element, String attributeName) {
        if (element == null) {
            return null;
        }
        String value = element.attr(attributeName);
        return StringUtils.hasText(value) ? value : null;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "limit은 1~100 범위여야 합니다.");
        }
        return limit;
    }

    private void sleepBeforeNextCall() {
        try {
            Thread.sleep(250L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.CRAWLING_FAILED, "토스 카드라운지 크롤링이 중단되었습니다.");
        }
    }

    public record TossCardSummary(
            String cardId,
            String cardName,
            String summary,
            String imageUrl,
            String detailUrl
    ) {
    }

    public record TossCardDetail(
            String annualFeeText,
            String previousMonthRequirement,
            List<String> benefitTexts,
            String rawDetailText
    ) {

        public static TossCardDetail empty() {
            return new TossCardDetail(null, null, List.of(), null);
        }
    }

    public record TossCardPayload(
            String cardId,
            String cardName,
            String summary,
            String imageUrl,
            String detailUrl,
            String annualFeeText,
            String previousMonthRequirement,
            List<String> benefitTexts,
            String rawDetailText
    ) {

        public String externalId() {
            return StringUtils.hasText(cardId) ? SOURCE_CODE + "|" + cardId : SOURCE_CODE + "|" + cardName;
        }

        public Map<String, Object> toRawPayload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("cardId", cardId);
            payload.put("cardName", cardName);
            payload.put("summary", summary);
            payload.put("imageUrl", imageUrl);
            payload.put("detailUrl", detailUrl);
            payload.put("annualFeeText", annualFeeText);
            payload.put("previousMonthRequirement", previousMonthRequirement);
            payload.put("benefitTexts", benefitTexts);
            payload.put("rawDetailText", rawDetailText);
            return payload;
        }
    }
}
