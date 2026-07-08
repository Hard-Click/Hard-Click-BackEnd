package com.wanted.backend.domain.quiz.domain.repository;

import com.wanted.backend.domain.quiz.domain.model.QuizSubmission;

public interface QuizSubmissionRepository {

    QuizSubmission save(QuizSubmission submission);

    boolean existsByQuizIdAndMemberId(Long quizId, Long memberId);
}
