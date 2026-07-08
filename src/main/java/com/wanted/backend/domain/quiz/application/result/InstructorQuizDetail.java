package com.wanted.backend.domain.quiz.application.result;

import java.time.LocalDateTime;
import java.util.List;

public record InstructorQuizDetail(
        Long quizId,
        String quizTitle,
        Long courseId,
        String courseTitle,
        Long sectionId,
        String sectionTitle,
        int questionCount,
        LocalDateTime createdAt,
        List<QuestionDetail> questions
) {
    public record QuestionDetail(
            Long questionId,
            int questionNumber,
            String questionText,
            Long correctOptionId,
            String explanation,
            List<OptionDetail> options
    ) {}

    public record OptionDetail(
            Long optionId,
            int optionNumber,
            String optionText,
            boolean correct
    ) {}
}
