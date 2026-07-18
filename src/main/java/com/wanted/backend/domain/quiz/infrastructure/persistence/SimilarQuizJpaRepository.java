package com.wanted.backend.domain.quiz.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SimilarQuizJpaRepository extends JpaRepository<SimilarQuizJpaEntity, Long> {
}
