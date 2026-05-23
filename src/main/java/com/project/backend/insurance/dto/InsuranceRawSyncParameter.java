package com.project.backend.insurance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/*
 * 보험 API 원본 적재 요청 파라미터입니다.
 * sourceCode를 지정하면 해당 보험 API만 호출하고, 비워두면 기본 보험 수집 흐름을 실행합니다.
 */
public record InsuranceRawSyncParameter(
        String sourceCode,
        @Min(1)
        Integer pageNo,
        @Min(1)
        @Max(100)
        Integer numOfRows,
        @Min(1)
        @Max(100)
        Integer maxPage,
        String resultType,
        String basDt,
        String beginBasDt,
        String endBasDt,
        String likeBasDt,
        String cmpyCd,
        String cmpyNm,
        String likeCmpyNm,
        String ptrn,
        String mog,
        String prdNm,
        String likePrdNm,
        String age,
        String ofrInstNm,
        String gdsNm,
        String insuNm,
        String upOrgCd,
        String orgCd,
        String mdfcnBgngYmd,
        String mdfcnEndYmd,
        String mode
) {
}
