package com.wanted.backend.domain.quiz.presentation.response;

import java.time.OffsetDateTime;
import java.util.List;

// 강사/관리자 퀴즈 통계 공용 응답. 특정 컨트롤러에 종속되지 않도록 top-level로 분리한다
// (강사 QuizController와 관리자 AdminQuizController가 함께 사용 → 컨트롤러 간 결합 방지).
public record InstructorQuizStatisticsResponse(
        String courseTitle,
        Long sectionId,
        String sectionTitle,
        int weekNumber,
        String quizTitle,
        Summary summary,
        List<ScoreDistribution> scoreDistribution,
        List<StudentScore> students
) {
    public record Summary(int totalCount, int submittedCount, int notSubmittedCount, int averageScore) {
    }

    public record ScoreDistribution(String range, int count, int percentage) {
    }

    public record StudentScore(
            String userId,
            String name,
            boolean submitted,
            Integer score,
            OffsetDateTime submittedAt
    ) {
    }
}
