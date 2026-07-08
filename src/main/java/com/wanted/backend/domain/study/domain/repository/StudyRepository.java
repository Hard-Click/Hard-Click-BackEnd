package com.wanted.backend.domain.study.domain.repository;

import com.wanted.backend.domain.study.domain.model.Study;

public interface StudyRepository {
    Study save(Study study);
}
