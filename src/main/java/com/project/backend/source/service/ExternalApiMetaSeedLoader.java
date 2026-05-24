package com.project.backend.source.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.source.document.ExternalApiMetaDocument;
import com.project.backend.source.dto.ExternalApiMetaSeed;
import com.project.backend.source.repository.ExternalApiMetaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Component
public class ExternalApiMetaSeedLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiMetaSeedLoader.class);

    private final ObjectMapper objectMapper;
    private final ExternalApiMetaRepository externalApiMetaRepository;
    private final Resource seedFile;

    public ExternalApiMetaSeedLoader(
            ObjectMapper objectMapper,
            ExternalApiMetaRepository externalApiMetaRepository,
            @Value("${external-api.meta.seed-file:classpath:seed/external-api-metas.json}") Resource seedFile
    ) {
        this.objectMapper = objectMapper;
        this.externalApiMetaRepository = externalApiMetaRepository;
        this.seedFile = seedFile;
    }

    /*
     * 로컬 seed JSON을 기준으로 external_api_metas를 upsert합니다.
     * seed 파일은 gitignore 대상이므로 없을 경우 서버 시작을 막지 않고 건너뜁니다.
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!seedFile.exists()) {
            log.info("External API meta seed file does not exist. path={}", seedFile);
            return;
        }

        List<ExternalApiMetaSeed> seeds = readSeeds();
        for (ExternalApiMetaSeed seed : seeds) {
            upsert(seed);
        }
        log.info("External API meta seed loaded. count={}", seeds.size());
    }

    private List<ExternalApiMetaSeed> readSeeds() {
        try {
            return objectMapper.readValue(
                    seedFile.getInputStream(),
                    new TypeReference<>() {
                    }
            );
        } catch (IOException exception) {
            throw new IllegalStateException("external-api-metas.json 파일을 읽을 수 없습니다.", exception);
        }
    }

    private void upsert(ExternalApiMetaSeed seed) {
        validateSeed(seed);
        externalApiMetaRepository.findBySourceCode(seed.sourceCode())
                .ifPresentOrElse(
                        source -> update(source, seed),
                        () -> create(seed)
                );
    }

    private void validateSeed(ExternalApiMetaSeed seed) {
        if (!StringUtils.hasText(seed.sourceCode())) {
            throw new IllegalStateException("external-api-metas.json: sourceCode는 필수입니다.");
        }
    }

    private void update(ExternalApiMetaDocument source, ExternalApiMetaSeed seed) {
        source.updateFromSeed(
                seed.sourceName(),
                seed.sourceType(),
                seed.category(),
                seed.baseUrl(),
                seed.authType(),
                seed.enabled(),
                seed.syncCycle()
        );
        externalApiMetaRepository.save(source);
    }

    private void create(ExternalApiMetaSeed seed) {
        externalApiMetaRepository.save(new ExternalApiMetaDocument(
                seed.sourceCode(),
                seed.sourceName(),
                seed.sourceType(),
                seed.category(),
                seed.baseUrl(),
                seed.authType(),
                seed.enabled(),
                seed.syncCycle()
        ));
    }
}
