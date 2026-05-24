package com.project.backend.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI moneyLogApiConnectorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MoneyLog API-Connector API")
                        .description("외부 청년정책, 보험, 카드 데이터를 수집하고 MongoDB 원본 데이터를 조회하는 내부 API 명세입니다.")
                        .version("v1"));
    }
}
