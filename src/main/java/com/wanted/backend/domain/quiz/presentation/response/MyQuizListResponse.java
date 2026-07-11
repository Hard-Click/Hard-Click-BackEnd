package com.wanted.backend.domain.quiz.presentation.response;

import java.time.OffsetDateTime;
import java.util.List;

public record MyQuizListResponse(
        Long courseId,
        String courseTitle,
        Summary summary,
        List<MyQuizItem> quizzes
) {
    public record Summary(int completedCount, int averageScore) {
    }

    public record MyQuizItem(
            Long quizId,
            int weekNumber,
            String quizTitle,
            int questionCount,
            boolean completed,
            Integer score,
            OffsetDateTime submittedAt
    ) {
    }
}
