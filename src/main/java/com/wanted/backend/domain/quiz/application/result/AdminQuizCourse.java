package com.wanted.backend.domain.quiz.application.result;

import java.time.Instant;

// 관리자 퀴즈 관리 강의 목록의 강의 1건 (강의 메타 + 수강생 수 + 강사명).
public record AdminQuizCourse(
        Long courseId,
        String courseTitle,
        boolean visible,
        int studentCount,
        String instructorName,
        Instant registeredAt
) {}
