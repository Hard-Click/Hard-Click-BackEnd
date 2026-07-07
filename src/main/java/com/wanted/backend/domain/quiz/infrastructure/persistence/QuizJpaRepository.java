package com.wanted.backend.domain.quiz.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizJpaRepository extends JpaRepository<QuizJpaEntity, Long> {

    // 단일 퀴즈 조회는 fetch join으로 문항까지 한 번에 로딩한다 (N+1 결정 순서: 단일→fetch join).
    // 문항별 options까지 한 쿼리에 중첩 fetch join하면 MultipleBagFetchException이 나므로
    // options는 컬렉션 @BatchSize로 IN 조회한다 (다중→batch size).
    @Query("select q from QuizJpaEntity q left join fetch q.questions where q.id = :id")
    Optional<QuizJpaEntity> findByIdWithQuestions(@Param("id") Long id);
}
