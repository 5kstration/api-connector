package com.project.backend.source.repository;

import com.project.backend.source.document.ExternalApiMetaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalApiMetaRepository extends MongoRepository<ExternalApiMetaDocument, String> {

    // sourceCode는 외부 출처를 구분하는 고유 키로 사용
    Optional<ExternalApiMetaDocument> findBySourceCode(String sourceCode);

    // 스케줄러나 관리 API에서 활성화된 출처만 조회할 때 사용
    List<ExternalApiMetaDocument> findAllByEnabledTrue();
}
