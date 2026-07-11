package com.wanted.backend.domain.quiz.presentation.response;

import java.time.OffsetDateTime;
import java.util.List;

public record InstructorQuizListResponse(
        Long courseId,
        Long sectionId,
        List<InstructorQuizItem> quizzes
) {
    public record InstructorQuizItem(
            Long quizId,
            String quizTitle,
            String courseTitle,
            Long sectionId,
            int weekNumber,
            String sectionTitle,
            int questionCount,
            OffsetDateTime createdAt
    ) {
    }
}
