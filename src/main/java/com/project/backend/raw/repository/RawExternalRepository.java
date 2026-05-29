package com.project.backend.raw.repository;

import com.project.backend.raw.document.RawExternalDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RawExternalRepository extends MongoRepository<RawExternalDocument, String> {

    // 같은 sourceCode에서 같은 rawHash가 있으면 이미 저장된 원본 데이터로 판단합니다.
    Optional<RawExternalDocument> findBySourceCodeAndRawHash(String sourceCode, String rawHash);

    // 특정 외부 출처에서 수집된 원본 데이터 전체를 확인할 때 사용합니다.
    List<RawExternalDocument> findAllBySourceCode(String sourceCode);
}
