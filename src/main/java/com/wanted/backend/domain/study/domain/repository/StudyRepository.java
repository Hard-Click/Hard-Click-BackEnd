package com.wanted.backend.domain.study.domain.repository;

import com.wanted.backend.domain.study.domain.model.Study;

import java.util.List;
import java.util.Optional;

public interface StudyRepository {
    Study save(Study study);

    Optional<Study> findById(Long id);

    List<Study> findAll(String subject, int page, int size);

    int countAll(String subject);
}
