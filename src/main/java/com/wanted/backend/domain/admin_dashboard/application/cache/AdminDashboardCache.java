package com.wanted.backend.domain.admin_dashboard.application.cache;

/**
 * 관리자 대시보드 요약 캐시의 이름/키 상수.
 * 캐시를 채우는 쪽(@Cacheable)과 무효화하는 쪽(이벤트 리스너)이 동일한 값을 쓰도록 한 곳에 모은다.
 */
public final class AdminDashboardCache {

    public static final String CACHE_NAME = "adminDashboard:v2";
    public static final String SUMMARY_KEY = "summary";

    private AdminDashboardCache() {
    }
}
