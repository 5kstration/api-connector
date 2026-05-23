package com.project.backend.sync.controller;

import com.project.backend.global.response.CommonResponse;
import com.project.backend.insurance.dto.InsuranceRawSyncParameter;
import com.project.backend.insurance.dto.InsuranceRawSyncResultResponse;
import com.project.backend.insurance.service.InsuranceRawSyncService;
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
    private final InsuranceRawSyncService insuranceRawSyncService;

    public SyncController(
            YouthPolicyRawSyncService youthPolicyRawSyncService,
            InsuranceRawSyncService insuranceRawSyncService
    ) {
        this.youthPolicyRawSyncService = youthPolicyRawSyncService;
        this.insuranceRawSyncService = insuranceRawSyncService;
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

    /*
     * 보험 API를 수동 호출하여 원본 데이터를 raw_externals에 저장합니다.
     */
    @PostMapping("/insurance-products")
    public CommonResponse<InsuranceRawSyncResultResponse> syncInsuranceProducts(
            @Valid @RequestBody(required = false) InsuranceRawSyncParameter parameter
    ) {
        return CommonResponse.success(
                insuranceRawSyncService.syncRaw(parameter),
                "보험 원본 데이터 적재가 완료되었습니다."
        );
    }
}
