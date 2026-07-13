package com.wanted.backend.domain.quiz.presentation.response;

import java.time.OffsetDateTime;
import java.util.List;

public record InstructorQuizDetailResponse(
        Long quizId,
        String quizTitle,
        Long courseId,
        String courseTitle,
        Long sectionId,
        String sectionTitle,
        int questionCount,
        OffsetDateTime createdAt,
        List<Question> questions
) {
    public record Question(
            Long questionId,
            int questionNumber,
            String questionText,
            Long correctOptionId,
            String explanation,
            Integer difficulty,
            List<Option> options
    ) {
    }

    public record Option(Long optionId, int optionNumber, String optionText, boolean correct) {
    }
}
