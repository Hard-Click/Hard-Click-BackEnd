package com.wanted.backend.domain.quiz.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SimilarQuizJpaRepository extends JpaRepository<SimilarQuizJpaEntity, Long> {

    // 문항 참조 목록을 함께 로딩해 findById의 +1 조회를 없앤다 (단일 조회 → fetch join).
    @EntityGraph(attributePaths = "questions")
    Optional<SimilarQuizJpaEntity> findWithQuestionsById(Long id);
}
