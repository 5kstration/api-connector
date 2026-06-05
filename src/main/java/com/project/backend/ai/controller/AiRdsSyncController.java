package com.project.backend.ai.controller;

import com.project.backend.ai.dto.AiRdsSyncResultResponse;
import com.project.backend.ai.service.AiRdsSyncScriptService;
import com.project.backend.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/ai-rds-sync")
@Tag(name = "AI RDS Sync", description = "MongoDB raw 데이터를 Python 스크립트로 정제해 AI-Service RDS에 저장하는 API")
public class AiRdsSyncController {

    private final AiRdsSyncScriptService aiRdsSyncScriptService;

    public AiRdsSyncController(AiRdsSyncScriptService aiRdsSyncScriptService) {
        this.aiRdsSyncScriptService = aiRdsSyncScriptService;
    }

    @Operation(
            summary = "AI RDS 동기화 수동 실행",
            description = "MongoDB raw_externals 데이터를 Python 스크립트로 정제하고 AI-Service RDS PostgreSQL 테이블에 저장합니다."
    )
    @PostMapping("/run")
    public CommonResponse<AiRdsSyncResultResponse> run() {
        return CommonResponse.success(
                aiRdsSyncScriptService.run(),
                "AI RDS 동기화 스크립트 실행이 완료되었습니다."
        );
    }
}
