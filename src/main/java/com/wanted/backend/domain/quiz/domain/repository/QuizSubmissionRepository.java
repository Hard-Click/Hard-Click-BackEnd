package com.wanted.backend.domain.quiz.domain.repository;

import com.wanted.backend.domain.quiz.domain.model.QuizSubmission;

import java.util.List;

public interface QuizSubmissionRepository {

    QuizSubmission save(QuizSubmission submission);

    boolean existsByQuizIdAndMemberId(Long quizId, Long memberId);

    List<QuizSubmission> findByMemberIdAndQuizIdIn(Long memberId, List<Long> quizIds);
}
