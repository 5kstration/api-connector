package com.project.backend.insurance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.insurance.dto.SafeInsuranceRegion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Component
public class SafeInsuranceRegionProvider {

    private final ObjectMapper objectMapper;
    private final Resource regionFile;

    public SafeInsuranceRegionProvider(
            ObjectMapper objectMapper,
            @Value("${external-api.insurance.safe-insurance.region-file:classpath:seed/safe-insurance-seoul-regions.json}")
            Resource regionFile
    ) {
        this.objectMapper = objectMapper;
        this.regionFile = regionFile;
    }

    /*
     * 시민안전보험 수집 대상 지역을 로컬 JSON에서 읽습니다.
     * enabled=true이고 시도/시군구 코드가 모두 있는 항목만 반환합니다.
     */
    public List<SafeInsuranceRegion> findEnabledRegions() {
        if (!regionFile.exists()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "시민안전보험 지역 코드 JSON 파일이 없습니다.");
        }

        try {
            List<SafeInsuranceRegion> regions = objectMapper.readValue(
                    regionFile.getInputStream(),
                    new TypeReference<>() {
                    }
            );
            return regions.stream()
                    .filter(SafeInsuranceRegion::enabled)
                    .filter(region -> StringUtils.hasText(region.upOrgCd()))
                    .filter(region -> StringUtils.hasText(region.orgCd()))
                    .toList();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "시민안전보험 지역 코드 JSON 파일을 읽을 수 없습니다.");
        }
    }
}
