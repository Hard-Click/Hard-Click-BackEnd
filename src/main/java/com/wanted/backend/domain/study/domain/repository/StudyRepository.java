package com.wanted.backend.domain.study.domain.repository;

import com.wanted.backend.domain.study.domain.model.Study;

import java.util.List;

public interface StudyRepository {
    Study save(Study study);

    List<Study> findAll(String subject, int page, int size);

    int countAll(String subject);
}
