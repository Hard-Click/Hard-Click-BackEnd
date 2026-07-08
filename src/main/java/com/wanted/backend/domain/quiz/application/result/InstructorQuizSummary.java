package com.wanted.backend.domain.quiz.application.result;

import java.time.LocalDateTime;

public record InstructorQuizSummary(
        Long quizId,
        String quizTitle,
        Long courseId,
        String courseTitle,
        Long sectionId,
        String sectionTitle,
        int questionCount,
        LocalDateTime createdAt
) {}
