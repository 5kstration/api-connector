package com.project.backend.sync.scheduler;

import com.project.backend.ai.service.AiRdsSyncScriptService;
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
    private final AiRdsSyncScriptService aiRdsSyncScriptService;

    public ExternalDataSyncScheduler(
            YouthPolicyRawSyncService youthPolicyRawSyncService,
            InsuranceRawSyncService insuranceRawSyncService,
            CardRawSyncService cardRawSyncService,
            AiRdsSyncScriptService aiRdsSyncScriptService
    ) {
        this.youthPolicyRawSyncService = youthPolicyRawSyncService;
        this.insuranceRawSyncService = insuranceRawSyncService;
        this.cardRawSyncService = cardRawSyncService;
        this.aiRdsSyncScriptService = aiRdsSyncScriptService;
    }

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

    @Scheduled(
            cron = "${sync.insurance-product.cron}",
            zone = "${sync.insurance-product.zone:Asia/Seoul}"
    )
    public void syncInsuranceProducts() {
        insuranceRawSyncService.syncRaw(InsuranceRawSyncParameter.forScheduler());
    }

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

    @Scheduled(
            cron = "${sync.ai-rds.cron:0 0 9 * * *}",
            zone = "${sync.ai-rds.zone:Asia/Seoul}"
    )
    public void syncAiRds() {
        aiRdsSyncScriptService.run();
    }
}
