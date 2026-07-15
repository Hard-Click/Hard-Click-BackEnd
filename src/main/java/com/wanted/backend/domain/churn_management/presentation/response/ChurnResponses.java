package com.wanted.backend.domain.churn_management.presentation.response;

import com.wanted.backend.domain.churn_management.application.dto.ChurnDashboardDtos;
import com.wanted.backend.domain.churn_management.application.dto.ChurnReasonType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * churn 대시보드 응답 모음. 도메인 스케일(risk_score 0~1)을 화면 스케일(0~100 정수)로 변환해 내보낸다.
 */
public final class ChurnResponses {

    private ChurnResponses() {
    }

    private static int toScore100(double score0to1) {
        return (int) Math.round(score0to1 * 100);
    }

    @Schema(description = "이탈관리 대시보드 상단 요약")
    public record SummaryResponse(
            @Schema(description = "고위험 학생 수", example = "18") long highRiskCount,
            @Schema(description = "중위험 학생 수", example = "34") long mediumRiskCount,
            @Schema(description = "이번 주 신규 위험 학생 수", example = "7") long newThisWeekCount,
            @Schema(description = "평균 위험 점수(0~100)", example = "42") int avgRiskScore
    ) {
        public static SummaryResponse from(ChurnDashboardDtos.Summary result) {
            return new SummaryResponse(
                    result.highRiskCount(),
                    result.mediumRiskCount(),
                    result.newThisWeekCount(),
                    toScore100(result.avgRiskScore())
            );
        }
    }

    @Schema(description = "이탈 위험 학생 추이 포인트(주 단위)")
    public record TrendPointResponse(
            @Schema(description = "주 시작일(월요일)", example = "2026-07-06") LocalDate weekStart,
            @Schema(description = "해당 주 고위험 학생 수", example = "16") long highRiskCount
    ) {
        public static TrendPointResponse from(ChurnDashboardDtos.TrendPoint point) {
            return new TrendPointResponse(point.weekStart(), point.highRiskCount());
        }
    }

    @Schema(description = "주요 이탈 사유 비율")
    public record ReasonResponse(
            @Schema(description = "사유 코드", example = "streak") String reasonCode,
            @Schema(description = "사유 라벨", example = "진도 밀림") String reasonLabel,
            @Schema(description = "해당 사유 학생 수", example = "41") long count,
            @Schema(description = "비율(0~100)", example = "41") int ratioPercent
    ) {
        public static ReasonResponse from(ChurnDashboardDtos.ReasonRatio reason) {
            return new ReasonResponse(
                    reason.reasonCode(),
                    reason.reasonLabel(),
                    reason.count(),
                    (int) Math.round(reason.ratio() * 100)
            );
        }
    }

    @Schema(description = "이탈 위험 학생 목록 행")
    public record StudentResponse(
            @Schema(description = "수강 ID", example = "1063") Long enrollmentId,
            @Schema(description = "회원 ID", example = "512") Long memberId,
            @Schema(description = "학생 이름", example = "김민수") String memberName,
            @Schema(description = "위험도(HIGH/MEDIUM/LOW)", example = "HIGH") String level,
            @Schema(description = "위험 점수(0~100)", example = "81") int riskScore,
            @Schema(description = "대표 사유 코드", example = "recency") String reasonCode,
            @Schema(description = "대표 사유 라벨", example = "장기 미접속") String reasonLabel,
            @Schema(description = "최근 활동일", example = "2026-07-02") LocalDate lastActivityDate,
            @Schema(description = "위험 점수 산출 시각") LocalDateTime computedAt
    ) {
        public static StudentResponse from(ChurnDashboardDtos.StudentRow row) {
            return new StudentResponse(
                    row.enrollmentId(),
                    row.memberId(),
                    row.memberName(),
                    row.level(),
                    toScore100(row.riskScore()),
                    row.topReasonCode(),
                    row.topReasonLabel(),
                    row.lastActivityDate(),
                    row.computedAt()
            );
        }
    }

    @Schema(description = "이탈 위험 학생 목록(페이지)")
    public record StudentPageResponse(
            List<StudentResponse> content,
            int page,
            int size,
            long totalElements
    ) {
        public static StudentPageResponse from(ChurnDashboardDtos.StudentPage pageResult) {
            return new StudentPageResponse(
                    pageResult.content().stream().map(StudentResponse::from).toList(),
                    pageResult.page(),
                    pageResult.size(),
                    pageResult.totalElements()
            );
        }
    }

    @Schema(description = "위험 점수 기여 요인")
    public record ContributionResponse(
            @Schema(description = "요인 코드", example = "recency") String code,
            @Schema(description = "요인 라벨", example = "장기 미접속") String label,
            @Schema(description = "총점 기여량(0~100)", example = "38") int points
    ) {
    }

    @Schema(description = "학생 위험 상세")
    public record StudentDetailResponse(
            @Schema(description = "수강 ID", example = "1063") Long enrollmentId,
            @Schema(description = "회원 ID", example = "512") Long memberId,
            @Schema(description = "학생 이름", example = "정하늘") String memberName,
            @Schema(description = "이메일", example = "stud1063@flown.dev") String email,
            @Schema(description = "위험도", example = "HIGH") String level,
            @Schema(description = "위험 점수(0~100)", example = "88") int riskScore,
            @Schema(description = "산출 시각") LocalDateTime computedAt,
            @Schema(description = "위험 점수 기여 요인(내림차순)") List<ContributionResponse> contributions,
            @Schema(description = "진도율(0~100, 미산출 시 null)", example = "38") Integer progressRate,
            @Schema(description = "마지막 접속일", example = "2026-06-29") LocalDate lastAccessDate,
            @Schema(description = "최근 퀴즈 평균(연동 전 null)", example = "48") Double recentQuizAvg,
            @Schema(description = "누적 순공 시간(분)", example = "900") Integer totalStudyMinutes
    ) {
        public static StudentDetailResponse from(ChurnDashboardDtos.StudentDetail detail) {
            List<ContributionResponse> contributions = detail.contributions().entrySet().stream()
                    .map(entry -> new ContributionResponse(
                            entry.getKey(),
                            ChurnReasonType.labelOf(entry.getKey()),
                            toScore100(entry.getValue())))
                    .sorted(Comparator.comparingInt(ContributionResponse::points).reversed())
                    .toList();

            return new StudentDetailResponse(
                    detail.enrollmentId(),
                    detail.memberId(),
                    detail.memberName(),
                    detail.email(),
                    detail.level(),
                    toScore100(detail.riskScore()),
                    detail.computedAt(),
                    contributions,
                    detail.progressRate() == null ? null : (int) Math.round(detail.progressRate() * 100),
                    detail.lastAccessDate(),
                    detail.recentQuizAvg(),
                    detail.totalStudyMinutes()
            );
        }
    }
}
