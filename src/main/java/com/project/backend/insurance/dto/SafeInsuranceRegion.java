package com.project.backend.insurance.dto;

/*
 * 시민안전보험 지역 코드 JSON을 읽기 위한 DTO입니다.
 * 실제 API 호출에는 upOrgCd와 orgCd를 사용합니다.
 */
public record SafeInsuranceRegion(
        String upOrgCd,
        String upOrgNm,
        String orgCd,
        String orgNm,
        boolean enabled
) {
}
