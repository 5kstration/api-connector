package com.project.backend.card.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TossCardLoungeCrawlerTest {

    private final TossCardLoungeCrawler crawler =
            new TossCardLoungeCrawler("https://card-lounge.toss.im/");

    @Test
    @DisplayName("토스 카드 목록 HTML에서 카드 ID와 카드명을 추출한다")
    void parseListExtractsCardIdAndCardName() {
        Document document = Jsoup.parse("""
                <html>
                  <body>
                    <div role="tabpanel" aria-labelledby="radix-r-v4-trigger-KB국민">
                      <ul>
                        <a href="/card/1121">
                          <img alt="KB국민 굿데이카드" src="https://static.toss.im/assets/credit-card/1121.png">
                          <p data-desktop-list-item-title>KB국민 굿데이카드</p>
                          <span>3</span>
                          <span data-desktop-list-item-text>연 최대 70만8천원 할인</span>
                          <span>79만원 이벤트</span>
                        </a>
                      </ul>
                    </div>
                    <div role="tabpanel" aria-labelledby="radix-r-v4-trigger-현대">
                      <ul>
                        <a href="/card/1827">
                          <img alt="현대카드 M" src="https://static.toss.im/assets/credit-card/1827.png">
                          <p data-desktop-list-item-title>현대카드 M</p>
                          <span>1</span>
                          <span data-desktop-list-item-text>적립에 적립을 더한 카드</span>
                          <span>연회비캐시백</span>
                        </a>
                      </ul>
                    </div>
                    <div role="tabpanel" aria-labelledby="radix-r-v4-trigger-20대">
                      <ul>
                        <a href="/card/10279">
                          <img alt="토스 삼성카드" src="https://static.toss.im/assets/credit-card/toss_samsung.png">
                          <p data-desktop-list-item-title>토스 삼성카드</p>
                          <span>1</span>
                          <span data-desktop-list-item-text>토스/온라인 영역 15%/10% 할인</span>
                        </a>
                      </ul>
                    </div>
                    <div role="tabpanel" aria-labelledby="radix-r-v4-trigger-40대">
                      <ul>
                        <a href="/card/9999">
                          <img alt="40대 테스트카드" src="https://static.toss.im/assets/credit-card/test.png">
                          <p data-desktop-list-item-title>40대 테스트카드</p>
                          <span>1</span>
                          <span data-desktop-list-item-text>청년 대상 제외 카드</span>
                        </a>
                      </ul>
                    </div>
                  </body>
                </html>
                """, "https://card-lounge.toss.im/");

        var result = crawler.parseList(document, 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).cardId()).isEqualTo("1121");
        assertThat(result.get(0).cardCompany()).isEqualTo("KB국민");
        assertThat(result.get(0).sourceSectionType()).isEqualTo("CARD_COMPANY");
        assertThat(result.get(0).sourceSectionName()).isEqualTo("KB국민");
        assertThat(result.get(0).rank()).isEqualTo(3);
        assertThat(result.get(0).cardName()).isEqualTo("KB국민 굿데이카드");
        assertThat(result.get(0).detailUrl()).isEqualTo("https://card-lounge.toss.im/card/1121");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://static.toss.im/assets/credit-card/1121.png");
        assertThat(result.get(0).summary()).isEqualTo("연 최대 70만8천원 할인");
        assertThat(result.get(0).badgeText()).isEqualTo("79만원 이벤트");
        assertThat(result.get(1).cardCompany()).isEqualTo("현대");
        assertThat(result.get(1).sourceSectionType()).isEqualTo("CARD_COMPANY");
        assertThat(result.get(1).sourceSectionName()).isEqualTo("현대");
        assertThat(result.get(1).rank()).isEqualTo(1);
        assertThat(result.get(1).summary()).isEqualTo("적립에 적립을 더한 카드");
        assertThat(result.get(1).badgeText()).isEqualTo("연회비캐시백");
        assertThat(result.get(2).cardCompany()).isNull();
        assertThat(result.get(2).sourceSectionType()).isEqualTo("AGE_GROUP");
        assertThat(result.get(2).sourceSectionName()).isEqualTo("20대");
        assertThat(result.get(2).rank()).isEqualTo(1);
        assertThat(result.get(2).cardName()).isEqualTo("토스 삼성카드");
        assertThat(result.get(2).summary()).isEqualTo("토스/온라인 영역 15%/10% 할인");
    }

    @Test
    @DisplayName("토스 카드 상세 HTML에서 혜택 안내 텍스트를 추출한다")
    void parseDetailExtractsBenefitTexts() {
        Document document = Jsoup.parse("""
                <html>
                  <body>
                    <main>
                      <p>연회비 국내전용 : 5,000원 국내외겸용 : 10,000원</p>
                      <p>전월실적 30만원 이상</p>
                      <section>
                        <h1>혜택 안내</h1>
                        <div>
                          <button>
                            <span>전기요금, 도시가스요금, 통신요금 할인</span>
                            <span>월납(공과금) 할인</span>
                            <span>자세히 보기</span>
                          </button>
                        </div>
                        <div>
                          <button>
                            <span>365일 24시간 10% 할인서비스</span>
                            <span>TIME 할인(편의점, 병원/약국, 세탁소 업종)</span>
                            <span>자세히 보기</span>
                          </button>
                        </div>
                      </section>
                    </main>
                  </body>
                </html>
                """);

        var result = crawler.parseDetail(document);

        assertThat(result.annualFeeText()).contains("국내전용");
        assertThat(result.previousMonthRequirement()).contains("30만원 이상");
        assertThat(result.benefitTexts())
                .containsExactly(
                        "전기요금, 도시가스요금, 통신요금 할인 / 월납(공과금) 할인",
                        "365일 24시간 10% 할인서비스 / TIME 할인(편의점, 병원/약국, 세탁소 업종)"
                );
    }
}
