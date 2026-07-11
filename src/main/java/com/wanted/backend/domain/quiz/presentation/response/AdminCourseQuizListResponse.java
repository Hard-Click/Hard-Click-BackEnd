package com.wanted.backend.domain.quiz.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "강의별 주차 퀴즈 목록")
public record AdminCourseQuizListResponse(
        @Schema(description = "강의 ID", example = "1") Long courseId,
        @Schema(description = "강의명", example = "React 완벽 가이드") String courseTitle,
        @Schema(description = "주차별 퀴즈 목록") List<WeeklyQuiz> weeks
) {
    @Schema(description = "주차별 퀴즈")
    public record WeeklyQuiz(
            @Schema(description = "퀴즈 ID", example = "90") Long quizId,
            @Schema(description = "주차", example = "1") int weekNumber,
            @Schema(description = "퀴즈명", example = "React 기초 개념") String quizTitle,
            @Schema(description = "등록 상태(등록된 퀴즈는 항상 완료)", example = "완료") String status,
            @Schema(description = "총 문제 수", example = "10") int totalQuestionCount,
            @Schema(description = "등록일시(응시일 필드명 유지)") OffsetDateTime examDate
    ) {
    }
}
