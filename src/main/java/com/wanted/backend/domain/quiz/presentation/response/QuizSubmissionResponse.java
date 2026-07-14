package com.wanted.backend.domain.quiz.presentation.response;

import java.time.OffsetDateTime;

public record QuizSubmissionResponse(
        Long submissionId,
        Long quizId,
        int score,
        int totalQuestionCount,
        int correctCount,
        int incorrectCount,
        OffsetDateTime submittedAt
) {
}
