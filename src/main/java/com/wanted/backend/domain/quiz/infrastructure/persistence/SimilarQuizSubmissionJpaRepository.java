package com.wanted.backend.domain.quiz.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SimilarQuizSubmissionJpaRepository
        extends JpaRepository<SimilarQuizSubmissionJpaEntity, Long> {
}
