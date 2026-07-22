package com.wanted.backend.domain.admin_dashboard.application.service;

import com.wanted.backend.domain.admin_dashboard.application.cache.AdminDashboardCache;
import com.wanted.backend.domain.admin_dashboard.application.dto.AdminDashboardResult;
import com.wanted.backend.domain.admin_dashboard.application.port.AdminDashboardQueryPort;
import com.wanted.backend.domain.admin_dashboard.application.usecase.GetAdminDashboardUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardQueryService implements GetAdminDashboardUseCase {

    private final AdminDashboardQueryPort adminDashboardQueryPort;

    @Override
    @Cacheable(cacheNames = AdminDashboardCache.CACHE_NAME, key = "'summary'", sync = true)
    public AdminDashboardResult getDashboard() {
        return adminDashboardQueryPort.findDashboard();
    }
}
