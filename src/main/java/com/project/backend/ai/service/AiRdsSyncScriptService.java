package com.project.backend.ai.service;

import com.project.backend.ai.dto.AiRdsSyncResultResponse;
import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AiRdsSyncScriptService {

    private static final Logger log = LoggerFactory.getLogger(AiRdsSyncScriptService.class);

    private final String pythonCommand;
    private final String scriptPath;
    private final String workingDirectory;
    private final long timeoutMinutes;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AiRdsSyncScriptService(
            @Value("${ai-rds-sync.python-command:python}") String pythonCommand,
            @Value("${ai-rds-sync.script-path:scripts/ai_rds_sync/sync_mongo_to_rds.py}") String scriptPath,
            @Value("${ai-rds-sync.working-directory:scripts/ai_rds_sync}") String workingDirectory,
            @Value("${ai-rds-sync.timeout-minutes:30}") long timeoutMinutes
    ) {
        this.pythonCommand = pythonCommand;
        this.scriptPath = scriptPath;
        this.workingDirectory = workingDirectory;
        this.timeoutMinutes = timeoutMinutes;
    }

    public AiRdsSyncResultResponse run() {
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException(ErrorCode.SYNC_ALREADY_RUNNING, "AI RDS 동기화 스크립트가 이미 실행 중입니다.");
        }

        LocalDateTime startedAt = LocalDateTime.now();
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command())
                    .directory(Path.of(workingDirectory).toFile());

            process = processBuilder.start();
            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI RDS 동기화 스크립트 실행 시간이 초과되었습니다.");
            }

            String output = read(process.getInputStream().readAllBytes());
            String errorOutput = read(process.getErrorStream().readAllBytes());
            int exitCode = process.exitValue();
            LocalDateTime endedAt = LocalDateTime.now();
            boolean success = exitCode == 0;

            if (success) {
                log.info("AI RDS sync script completed. durationSeconds={}", Duration.between(startedAt, endedAt).toSeconds());
            } else {
                log.warn("AI RDS sync script failed. exitCode={}, stderr={}", exitCode, errorOutput);
            }

            return new AiRdsSyncResultResponse(success, exitCode, startedAt, endedAt, output, errorOutput);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI RDS 동기화 스크립트를 실행할 수 없습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI RDS 동기화 스크립트 실행이 중단되었습니다.");
        } finally {
            running.set(false);
        }
    }

    private List<String> command() {
        return List.of(pythonCommand, scriptFileName());
    }

    private String scriptFileName() {
        Path path = Path.of(scriptPath);
        Path fileName = path.getFileName();
        return fileName == null ? scriptPath : fileName.toString();
    }

    private String read(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }
}
