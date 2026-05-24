package com.project.backend.sync.scheduler;

import com.project.backend.card.dto.CardRawSyncParameter;
import com.project.backend.card.service.CardRawSyncService;
import com.project.backend.insurance.dto.InsuranceRawSyncParameter;
import com.project.backend.insurance.service.InsuranceRawSyncService;
import com.project.backend.policy.dto.YouthPolicyParameter;
import com.project.backend.policy.service.YouthPolicyRawSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExternalDataSyncScheduler {

    private final YouthPolicyRawSyncService youthPolicyRawSyncService;
    private final InsuranceRawSyncService insuranceRawSyncService;
    private final CardRawSyncService cardRawSyncService;

    public ExternalDataSyncScheduler(
            YouthPolicyRawSyncService youthPolicyRawSyncService,
            InsuranceRawSyncService insuranceRawSyncService,
            CardRawSyncService cardRawSyncService
    ) {
        this.youthPolicyRawSyncService = youthPolicyRawSyncService;
        this.insuranceRawSyncService = insuranceRawSyncService;
        this.cardRawSyncService = cardRawSyncService;
    }

    /*
     * application.yml의 sync.youth-policy.cron 값에 맞춰 청년정책 원본 데이터를 적재합니다.
     */
    @Scheduled(
            cron = "${sync.youth-policy.cron}",
            zone = "${sync.youth-policy.zone:Asia/Seoul}"
    )
    public void syncYouthPolicies() {
        youthPolicyRawSyncService.syncRaw(new YouthPolicyParameter(
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    /*
     * application.yml의 sync.insurance-product.cron 값에 맞춰 보험 원본 데이터를 적재합니다.
     */
    @Scheduled(
            cron = "${sync.insurance-product.cron}",
            zone = "${sync.insurance-product.zone:Asia/Seoul}"
    )
    public void syncInsuranceProducts() {
        insuranceRawSyncService.syncRaw(InsuranceRawSyncParameter.forScheduler());
    }

    /*
     * application.yml의 sync.card-product.cron 값에 맞춰 카드 원본 데이터를 적재합니다.
     */
    @Scheduled(
            cron = "${sync.card-product.cron}",
            zone = "${sync.card-product.zone:Asia/Seoul}"
    )
    public void syncCardProducts() {
        cardRawSyncService.syncRaw(new CardRawSyncParameter(
                null,
                20
        ));
    }
}
