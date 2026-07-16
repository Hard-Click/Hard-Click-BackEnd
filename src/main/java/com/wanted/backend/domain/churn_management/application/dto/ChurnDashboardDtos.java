package com.wanted.backend.domain.churn_management.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * churn 대시보드 조회 결과 DTO 모음(application 레이어). 값은 도메인 스케일 그대로(risk_score 0~1) 담고,
 * 화면 스케일(×100) 변환은 presentation/response 에서 수행한다.
 */
public final class ChurnDashboardDtos {

    private ChurnDashboardDtos() {
    }

    /** 상단 카드 4개: 고위험/중위험 학생 수, 이번 주 신규 위험, 평균 위험 점수(0~1). */
    public record Summary(
            long highRiskCount,
            long mediumRiskCount,
            long newThisWeekCount,
            double avgRiskScore
    ) {
    }

    /** '이탈 위험 학생 추이' 차트의 주간 포인트. weekStart = 그 주 월요일. */
    public record TrendPoint(
            LocalDate weekStart,
            long highRiskCount
    ) {
    }

    /** '주요 이탈 사유' 비율. ratio = 0~1. */
    public record ReasonRatio(
            String reasonCode,
            String reasonLabel,
            long count,
            double ratio
    ) {
    }

    /** 학생 목록 행. level = HIGH/MEDIUM/LOW(risk_score 임계값 분류). */
    public record StudentRow(
            Long enrollmentId,
            Long memberId,
            String memberName,
            String level,
            double riskScore,
            String topReasonCode,
            String topReasonLabel,
            LocalDate lastActivityDate,
            LocalDateTime computedAt
    ) {
    }

    /** 학생 목록 페이지. */
    public record StudentPage(
            List<StudentRow> content,
            int page,
            int size,
            long totalElements
    ) {
    }

    /**
     * 학생 위험 상세.
     * contributions: 축별 기여도(recency/streak/quiz, 0~1) — features JSON 에서 파싱.
     * progressRate: schedule_slot DONE 비율(0~1, nullable).
     * recentQuizAvg: 퀴즈 도메인 연동 전이라 현재 null(TODO).
     * totalStudyMinutes: daily_achievement actual_min 누적.
     */
    public record StudentDetail(
            Long enrollmentId,
            Long memberId,
            String memberName,
            String email,
            String level,
            double riskScore,
            LocalDateTime computedAt,
            Map<String, Double> contributions,
            Double progressRate,
            LocalDate lastAccessDate,
            Double recentQuizAvg,
            Integer totalStudyMinutes
    ) {
    }
}
