package com.wanted.backend.domain.quiz.presentation.response;

import java.util.List;

public record StudentQuizDetailResponse(
        Long quizId,
        String quizTitle,
        String courseTitle,
        String sectionTitle,
        int totalQuestionCount,
        int answeredCount,
        boolean submitted,
        List<Question> questions
) {
    public record Question(Long questionId, int questionNumber, String questionText, List<Option> options) {
    }

    public record Option(Long optionId, int optionNumber, String optionText) {
    }
}
