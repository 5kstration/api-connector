package com.project.backend.raw.service;

import com.project.backend.global.exception.BusinessException;
import com.project.backend.global.exception.ErrorCode;
import com.project.backend.raw.document.RawExternalDocument;
import com.project.backend.raw.dto.RawExternalResponse;
import com.project.backend.raw.dto.RawExternalSearchCondition;
import com.project.backend.raw.repository.RawExternalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class RawExternalQueryService {

    private final RawExternalRepository rawExternalRepository;
    private final MongoTemplate mongoTemplate;

    public RawExternalQueryService(
            RawExternalRepository rawExternalRepository,
            MongoTemplate mongoTemplate
    ) {
        this.rawExternalRepository = rawExternalRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /*
     * raw_externals 목록을 조건 기반으로 조회합니다.
     * 기본 정렬은 fetchedAt 내림차순입니다.
     */
    public Page<RawExternalResponse> findAll(RawExternalSearchCondition condition) {
        PageRequest pageRequest = PageRequest.of(
                condition.page(),
                condition.size(),
                Sort.by(Sort.Direction.DESC, "fetchedAt")
        );
        Query query = query(condition).with(pageRequest);
        Query countQuery = query(condition);

        List<RawExternalResponse> contents = mongoTemplate.find(query, RawExternalDocument.class)
                .stream()
                .map(RawExternalResponse::from)
                .toList();
        long total = mongoTemplate.count(countQuery, RawExternalDocument.class);

        return new PageImpl<>(contents, pageRequest, total);
    }

    public RawExternalResponse findById(String id) {
        return rawExternalRepository.findById(id)
                .map(RawExternalResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.RAW_EXTERNAL_NOT_FOUND));
    }

    private Query query(RawExternalSearchCondition condition) {
        List<Criteria> criteria = new ArrayList<>();

        if (StringUtils.hasText(condition.sourceCode())) {
            criteria.add(Criteria.where("sourceCode").is(condition.sourceCode()));
        }
        if (StringUtils.hasText(condition.category())) {
            criteria.add(Criteria.where("category").is(condition.category()));
        }
        if (StringUtils.hasText(condition.externalId())) {
            criteria.add(Criteria.where("externalId").is(condition.externalId()));
        }
        if (StringUtils.hasText(condition.status())) {
            criteria.add(Criteria.where("status").is(condition.status()));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }
        return query;
    }
}
