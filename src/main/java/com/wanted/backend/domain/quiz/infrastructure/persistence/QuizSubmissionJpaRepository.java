package com.wanted.backend.domain.quiz.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSubmissionJpaRepository extends JpaRepository<QuizSubmissionJpaEntity, Long> {

    boolean existsByQuizIdAndMemberId(Long quizId, Long memberId);

    // 내 퀴즈 목록의 제출 여부/점수 조인용 — 강의 퀴즈 id 묶음으로 IN 조회 (N+1 방지).
    List<QuizSubmissionJpaEntity> findByMemberIdAndQuizIdIn(Long memberId, List<Long> quizIds);
}
