package com.wanted.backend.domain.quiz.application.result;

import java.time.LocalDateTime;

public record QuizSubmissionResult(
        Long submissionId,
        Long quizId,
        int score,
        int totalQuestionCount,
        int correctCount,
        int incorrectCount,
        LocalDateTime submittedAt
) {}
