package com.wanted.backend.domain.churn_management.application.service;

import com.wanted.backend.domain.churn_management.application.dto.ChurnDashboardDtos;
import com.wanted.backend.domain.churn_management.application.port.ChurnQueryPort;
import com.wanted.backend.domain.churn_management.application.usecase.GetChurnDashboardUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChurnQueryService implements GetChurnDashboardUseCase {

    private static final int DEFAULT_TREND_WEEKS = 8;
    private static final int MAX_PAGE_SIZE = 100;

    private final ChurnQueryPort churnQueryPort;

    @Override
    public ChurnDashboardDtos.Summary getSummary() {
        return churnQueryPort.findSummary();
    }

    @Override
    public List<ChurnDashboardDtos.TrendPoint> getTrend(int weeks) {
        int safeWeeks = weeks <= 0 ? DEFAULT_TREND_WEEKS : weeks;
        return churnQueryPort.findTrend(safeWeeks);
    }

    @Override
    public List<ChurnDashboardDtos.ReasonRatio> getReasons() {
        return churnQueryPort.findReasons();
    }

    @Override
    public ChurnDashboardDtos.StudentPage getStudents(String level, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        return churnQueryPort.findStudents(level, safePage, safeSize);
    }

    @Override
    public ChurnDashboardDtos.StudentDetail getStudentDetail(Long enrollmentId) {
        return churnQueryPort.findStudentDetail(enrollmentId);
    }
}
