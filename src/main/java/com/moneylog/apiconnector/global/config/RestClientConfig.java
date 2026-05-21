package com.moneylog.apiconnector.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// 폴더 gloabl: 프로젝트 전체 사용되는 파트

/* config / spring boot 설정 관리
외부 API 호출 시 RestClient 의 공통 Builder로 Bean 등록

 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
    /*
    private final RestClient restClient;
    DI로 사용하기 위해 작성
     */
}
