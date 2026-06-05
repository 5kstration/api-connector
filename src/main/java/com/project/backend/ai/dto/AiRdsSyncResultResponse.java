package com.project.backend.ai.dto;

import java.time.LocalDateTime;

public record AiRdsSyncResultResponse(
        boolean success,
        int exitCode,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String output,
        String errorOutput
) {
}
