package com.project.backend.raw.controller;

import com.project.backend.global.response.CommonResponse;
import com.project.backend.raw.dto.RawExternalResponse;
import com.project.backend.raw.dto.RawExternalSearchCondition;
import com.project.backend.raw.service.RawExternalQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/v1/raw-externals")
@Tag(name = "Raw External", description = "MongoDB raw_externals 원본 데이터 조회 API")
public class RawExternalController {

    private final RawExternalQueryService rawExternalQueryService;

    public RawExternalController(RawExternalQueryService rawExternalQueryService) {
        this.rawExternalQueryService = rawExternalQueryService;
    }

    /*
     * raw_externals 목록을 조회합니다.
     * sourceCode, category, externalId, status 조건을 선택적으로 사용할 수 있습니다.
     */
    @Operation(
            summary = "원본 외부 데이터 목록 조회",
            description = "sourceCode, category, externalId, status 조건으로 raw_externals 데이터를 페이지 조회합니다."
    )
    @GetMapping
    public CommonResponse<Page<RawExternalResponse>> findRawExternals(
            @Valid @ModelAttribute RawExternalSearchCondition condition
    ) {
        return CommonResponse.success(rawExternalQueryService.findAll(condition));
    }

    /*
     * raw_externals 단건을 MongoDB ObjectId 기준으로 조회합니다.
     */
    @Operation(
            summary = "원본 외부 데이터 단건 조회",
            description = "MongoDB ObjectId 기준으로 raw_externals 데이터를 조회합니다."
    )
    @GetMapping("/{id}")
    public CommonResponse<RawExternalResponse> findRawExternal(
            @Parameter(description = "raw_externals MongoDB ObjectId")
            @PathVariable
            @Pattern(regexp = "^[a-fA-F0-9]{24}$", message = "유효한 MongoDB ObjectId 형식이 아닙니다.")
            String id
    ) {
        return CommonResponse.success(rawExternalQueryService.findById(id));
    }
}
