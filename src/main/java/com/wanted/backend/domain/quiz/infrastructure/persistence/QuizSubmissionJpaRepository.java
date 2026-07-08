package com.wanted.backend.domain.quiz.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizSubmissionJpaRepository extends JpaRepository<QuizSubmissionJpaEntity, Long> {

    boolean existsByQuizIdAndMemberId(Long quizId, Long memberId);
}
