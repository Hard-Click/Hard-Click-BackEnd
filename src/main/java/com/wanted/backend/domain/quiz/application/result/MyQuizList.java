package com.wanted.backend.domain.quiz.application.result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 학생 '내 퀴즈 목록' — 선택한 강의의 주차별 퀴즈 + 내 응시 현황 요약.
 */
public record MyQuizList(
        Long courseId,
        String courseTitle,
        int completedCount,
        int averageScore,
        List<MyQuizItem> quizzes
) {
    public record MyQuizItem(
            Long quizId,
            int weekNumber,
            String quizTitle,
            int questionCount,
            boolean completed,
            Integer score,
            LocalDateTime submittedAt
    ) {}
}
