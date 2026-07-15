package com.wanted.backend.domain.churn_management.application.port;

import com.wanted.backend.domain.churn_management.application.dto.ChurnDashboardDtos;

/**
 * 이탈관리 조회 아웃바운드 포트. infrastructure 가 dropout_risk(=Python-Server 산출물) 등을 read 로 구현.
 * churn 도메인은 계산을 하지 않는다 — 야간 배치(Python)가 선계산해둔 값을 읽기만 한다.
 */
public interface ChurnQueryPort {

    ChurnDashboardDtos.Summary findSummary();

    java.util.List<ChurnDashboardDtos.TrendPoint> findTrend(int weeks);

    java.util.List<ChurnDashboardDtos.ReasonRatio> findReasons();

    ChurnDashboardDtos.StudentPage findStudents(String level, int page, int size);

    ChurnDashboardDtos.StudentDetail findStudentDetail(Long enrollmentId);

    /** enrollmentId -> memberId (알림 발송 대상 해석용). 없으면 null. */
    Long findMemberIdByEnrollmentId(Long enrollmentId);
}
