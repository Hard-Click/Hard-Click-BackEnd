package com.wanted.backend.domain.churn_management.application.usecase;

import com.wanted.backend.domain.churn_management.application.dto.ChurnDashboardDtos;

import java.util.List;

public interface GetChurnDashboardUseCase {

    ChurnDashboardDtos.Summary getSummary();

    List<ChurnDashboardDtos.TrendPoint> getTrend(int weeks);

    List<ChurnDashboardDtos.ReasonRatio> getReasons();

    ChurnDashboardDtos.StudentPage getStudents(String level, int page, int size);

    ChurnDashboardDtos.StudentDetail getStudentDetail(Long enrollmentId);
}
