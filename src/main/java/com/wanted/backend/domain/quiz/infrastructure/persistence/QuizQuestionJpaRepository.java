package com.wanted.backend.domain.quiz.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuizQuestionJpaRepository extends JpaRepository<QuizQuestionJpaEntity, Long> {

    // 유사퀴즈 채점(②): 저장된 문항 id만 직접 조회한다(코스 전체 로딩 회피).
    // 보기(options)는 @EntityGraph로 함께 로딩해 문항별 +1 조회를 없앤다.
    @EntityGraph(attributePaths = "options")
    List<QuizQuestionJpaEntity> findByIdIn(Collection<Long> ids);
}
