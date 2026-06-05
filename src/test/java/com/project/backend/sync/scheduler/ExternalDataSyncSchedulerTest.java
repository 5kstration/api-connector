package com.project.backend.sync.scheduler;

import com.project.backend.ai.service.AiRdsSyncScriptService;
import com.project.backend.card.service.CardRawSyncService;
import com.project.backend.global.config.SchedulerConfig;
import com.project.backend.insurance.service.InsuranceRawSyncService;
import com.project.backend.policy.service.YouthPolicyRawSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {
        SchedulerConfig.class,
        ExternalDataSyncScheduler.class
})
@TestPropertySource(properties = {
        "sync.youth-policy.cron=*/1 * * * * *",
        "sync.youth-policy.zone=Asia/Seoul",
        "sync.insurance-product.cron=*/1 * * * * *",
        "sync.insurance-product.zone=Asia/Seoul",
        "sync.card-product.cron=*/1 * * * * *",
        "sync.card-product.zone=Asia/Seoul",
        "sync.ai-rds.cron=*/1 * * * * *",
        "sync.ai-rds.zone=Asia/Seoul"
})
class ExternalDataSyncSchedulerTest {

    @MockitoBean
    private YouthPolicyRawSyncService youthPolicyRawSyncService;

    @MockitoBean
    private InsuranceRawSyncService insuranceRawSyncService;

    @MockitoBean
    private CardRawSyncService cardRawSyncService;

    @MockitoBean
    private AiRdsSyncScriptService aiRdsSyncScriptService;

    @Test
    @DisplayName("스케줄러 cron 설정에 따라 청년정책, 보험, 카드, AI RDS 동기화가 실행된다")
    void scheduledSyncRunsByCron() {
        verify(youthPolicyRawSyncService, timeout(3_000).atLeastOnce()).syncRaw(any());
        verify(insuranceRawSyncService, timeout(3_000).atLeastOnce()).syncRaw(any());
        verify(cardRawSyncService, timeout(3_000).atLeastOnce()).syncRaw(any());
        verify(aiRdsSyncScriptService, timeout(3_000).atLeastOnce()).run();

        verify(youthPolicyRawSyncService, atLeastOnce()).syncRaw(any());
        verify(insuranceRawSyncService, atLeastOnce()).syncRaw(any());
        verify(cardRawSyncService, atLeastOnce()).syncRaw(any());
        verify(aiRdsSyncScriptService, atLeastOnce()).run();
    }
}
