package com.project.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/api_connector_test",
        "external-api.youth-center.base-url=https://www.youthcenter.go.kr",
        "external-api.youth-center.policy-path=/go/ythip/getPlcy",
        "external-api.youth-center.api-key=test-youth-center-api-key",
        "external-api.youth-center.page-size=100",
        "external-api.youth-center.rtn-type=json",
        "external-api.youth-center.list-page-type=1",
        "external-api.card.toss-card-lounge.url=https://card-lounge.toss.im/",
        "external-api.insurance.data-go-kr-service-key=test-data-go-kr-service-key",
        "external-api.insurance.indemnity.base-url=http://apis.data.go.kr",
        "external-api.insurance.indemnity.path=/1160100/service/GetMedicalReimbursementInsuranceInfoService/getInsuranceInfo",
        "external-api.insurance.post-best.base-url=https://apis.data.go.kr",
        "external-api.insurance.post-best.path=/B552886/svc_postInsuBest/getPostInsuBestPrdt",
        "external-api.insurance.post-product.base-url=https://apis.data.go.kr",
        "external-api.insurance.post-product.path=/1721301/KrpostInsuranceProductView/InsuranceGoods",
        "external-api.insurance.post-coverage.base-url=https://apis.data.go.kr",
        "external-api.insurance.post-coverage.path=/1721301/KrpostInsuranceGuaranteeContentView/InsuranceGuranteeContent",
        "external-api.insurance.safe-insurance.base-url=https://www.ins24.go.kr",
        "external-api.insurance.safe-insurance.path=/api/safeInsrncInfoApi",
        "sync.youth-policy.cron=0 0 3 * * *",
        "sync.youth-policy.zone=Asia/Seoul",
        "sync.card-product.cron=0 0 4 * * *",
        "sync.card-product.zone=Asia/Seoul",
        "sync.insurance-product.cron=0 0 5 * * *",
        "sync.insurance-product.zone=Asia/Seoul"
})
class ApiConnectorApplicationTests {

    @Test
    void contextLoads() {
    }
}
