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
                    <ul>
                      <a href="/card/1121">
                        <img alt="KB국민 굿데이카드" src="https://static.toss.im/assets/credit-card/1121.png">
                        <p data-desktop-list-item-title>KB국민 굿데이카드</p>
                        <span>3</span>
                        <span data-desktop-list-item-text>연 최대 70만8천원 할인</span>
                      </a>
                    </ul>
                  </body>
                </html>
                """, "https://card-lounge.toss.im/");

        var result = crawler.parseList(document, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).cardId()).isEqualTo("1121");
        assertThat(result.get(0).cardName()).isEqualTo("KB국민 굿데이카드");
        assertThat(result.get(0).detailUrl()).isEqualTo("https://card-lounge.toss.im/card/1121");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://static.toss.im/assets/credit-card/1121.png");
        assertThat(result.get(0).summary()).isEqualTo("연 최대 70만8천원 할인");
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
