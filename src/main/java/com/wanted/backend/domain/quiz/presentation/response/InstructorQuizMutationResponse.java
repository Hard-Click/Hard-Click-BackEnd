package com.wanted.backend.domain.quiz.presentation.response;

import java.time.OffsetDateTime;

public record InstructorQuizMutationResponse(
        Long quizId,
        String quizTitle,
        int questionCount,
        OffsetDateTime updatedAt
) {
}
