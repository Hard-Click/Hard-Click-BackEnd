package com.wanted.backend.domain.quiz.presentation.response;

import java.time.OffsetDateTime;

public record InstructorQuizDeleteResponse(
        Long quizId,
        String status,
        OffsetDateTime deletedAt
) {
}
