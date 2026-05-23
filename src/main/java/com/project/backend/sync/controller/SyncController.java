package com.project.backend.sync.controller;

import com.project.backend.global.response.CommonResponse;
import com.project.backend.policy.dto.YouthPolicyParameter;
import com.project.backend.policy.dto.YouthPolicyRawSyncResultResponse;
import com.project.backend.policy.service.YouthPolicyRawSyncService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/sync")
public class SyncController {

    private final YouthPolicyRawSyncService youthPolicyRawSyncService;

    public SyncController(YouthPolicyRawSyncService youthPolicyRawSyncService) {
        this.youthPolicyRawSyncService = youthPolicyRawSyncService;
    }

    /*
     * 청년정책 API를 수동 호출하여 원본 데이터를 raw_externals에 저장합니다.
     */
    @PostMapping("/youth-policies")
    public CommonResponse<YouthPolicyRawSyncResultResponse> syncYouthPolicies(
            @Valid @RequestBody(required = false) YouthPolicyParameter parameter
    ) {
        return CommonResponse.success(
                youthPolicyRawSyncService.syncRaw(parameter),
                "청년정책 원본 데이터 적재가 완료되었습니다."
        );
    }
}
