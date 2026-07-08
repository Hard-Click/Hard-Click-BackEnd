package com.wanted.backend.domain.quiz.application.result;

import java.util.List;

/**
 * 학생 응시 화면용 퀴즈 상세 — 정답(correct)/해설(explanation)은 노출하지 않는다.
 */
public record StudentQuizDetail(
        Long quizId,
        String quizTitle,
        String courseTitle,
        String sectionTitle,
        int totalQuestionCount,
        int answeredCount,
        boolean submitted,
        List<Question> questions
) {
    public record Question(
            Long questionId,
            int questionNumber,
            String questionText,
            List<Option> options
    ) {}

    public record Option(
            Long optionId,
            int optionNumber,
            String optionText
    ) {}
}
