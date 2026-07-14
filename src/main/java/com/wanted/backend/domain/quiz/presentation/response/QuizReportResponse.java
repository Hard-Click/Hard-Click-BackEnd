package com.wanted.backend.domain.quiz.presentation.response;

import java.time.OffsetDateTime;
import java.util.List;

public record QuizReportResponse(
        Long quizId,
        int week,
        String quizTitle,
        OffsetDateTime submittedAt,
        int score,
        int totalScore,
        int correctCount,
        int incorrectCount,
        int scoreDiff,
        Integer previousScore,
        List<QuestionResult> wrongNotes,
        List<QuestionResult> questions
) {
    public record QuestionResult(
            Long questionId,
            int questionNumber,
            String questionText,
            Long correctOptionId,
            Long selectedOptionId,
            boolean correct,
            String explanation,
            List<Option> options
    ) {
    }

    public record Option(Long optionId, int optionNumber, String optionText) {
    }
}
