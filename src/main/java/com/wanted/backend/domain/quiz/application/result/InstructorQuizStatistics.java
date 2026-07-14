package com.wanted.backend.domain.quiz.application.result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 강사 퀴즈 점수 현황 통계.
 * 요약(전체/응시/미응시/평균), 점수 구간 분포, 수강생별 응시·점수 현황을 담는다.
 */
public record InstructorQuizStatistics(
        String courseTitle,
        Long sectionId,
        String sectionTitle,
        int weekNumber,
        String quizTitle,
        Summary summary,
        List<ScoreDistribution> scoreDistribution,
        List<StudentScore> students
) {
    public record Summary(int totalCount, int submittedCount, int notSubmittedCount, int averageScore) {}

    public record ScoreDistribution(String range, int count, int percentage) {}

    public record StudentScore(
            String userId,
            String name,
            boolean submitted,
            Integer score,
            LocalDateTime submittedAt
    ) {}
}
