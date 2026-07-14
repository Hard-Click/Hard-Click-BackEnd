package com.wanted.backend.domain.quiz.application.result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 학생 퀴즈 결과/오답노트 리포트.
 * 제출 완료 후 조회하므로 정답(correctOptionId)/해설(explanation)을 노출한다.
 */
public record QuizReport(
        Long quizId,
        int week,
        String quizTitle,
        LocalDateTime submittedAt,
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
            List<OptionView> options
    ) {}

    public record OptionView(
            Long optionId,
            int optionNumber,
            String optionText
    ) {}
}
