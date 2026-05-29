package com.project.backend.policy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/*
 * 온통청년 청년정책 API에 전달할 요청 파라미터입니다.
 * 이 DTO는 우리 서버의 응답 구조가 아니라 외부 API 호출 조건을 표현합니다.
 */
public record YouthPolicyParameter(
        @Min(1)
        Integer pageNum,
        @Min(1)
        @Max(100)
        Integer pageSize,
        String pageType,
        String rtnType,
        String policyNo,
        String keywordName,
        String policyDescription,
        String policyName,
        String regionCode,
        String largeCategoryName,
        String middleCategoryName
) {
}
