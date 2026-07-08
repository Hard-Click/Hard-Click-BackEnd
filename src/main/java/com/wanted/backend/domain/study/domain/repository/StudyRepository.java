package com.wanted.backend.domain.study.domain.repository;

import com.wanted.backend.domain.study.domain.model.Study;

import java.util.Optional;

public interface StudyRepository {
    Study save(Study study);

    Optional<Study> findById(Long id);
}
