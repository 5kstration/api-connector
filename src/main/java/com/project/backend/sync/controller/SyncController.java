package com.project.backend.sync.controller;

import com.project.backend.card.dto.CardRawSyncParameter;
import com.project.backend.card.dto.CardRawSyncResultResponse;
import com.project.backend.card.service.CardRawSyncService;
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
    private final CardRawSyncService cardRawSyncService;

    public SyncController(
            YouthPolicyRawSyncService youthPolicyRawSyncService,
            InsuranceRawSyncService insuranceRawSyncService,
            CardRawSyncService cardRawSyncService
    ) {
        this.youthPolicyRawSyncService = youthPolicyRawSyncService;
        this.insuranceRawSyncService = insuranceRawSyncService;
        this.cardRawSyncService = cardRawSyncService;
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

    /*
     * 카드 크롤링 데이터를 수동 적재하여 raw_externals에 저장합니다.
     */
    @PostMapping("/card-products")
    public CommonResponse<CardRawSyncResultResponse> syncCardProducts(
            @Valid @RequestBody(required = false) CardRawSyncParameter parameter
    ) {
        return CommonResponse.success(
                cardRawSyncService.syncRaw(parameter),
                "카드 원본 데이터 적재가 완료되었습니다."
        );
    }
}
