package com.project.backend.sync.scheduler;

import com.project.backend.policy.dto.YouthPolicyParameter;
import com.project.backend.policy.service.YouthPolicyRawSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExternalDataSyncScheduler {

    private final YouthPolicyRawSyncService youthPolicyRawSyncService;

    public ExternalDataSyncScheduler(YouthPolicyRawSyncService youthPolicyRawSyncService) {
        this.youthPolicyRawSyncService = youthPolicyRawSyncService;
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
}
