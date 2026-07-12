package com.wanted.backend.domain.quiz.application.result;

import java.time.LocalDateTime;
import java.util.List;

// 관리자 강의별 주차 퀴즈 목록 (강의 + 주차순 퀴즈).
public record AdminCourseQuizzes(
        Long courseId,
        String courseTitle,
        List<WeeklyQuiz> weeks
) {
    public record WeeklyQuiz(
            Long quizId,
            int weekNumber,
            String quizTitle,
            int questionCount,
            LocalDateTime createdAt
    ) {}
}
