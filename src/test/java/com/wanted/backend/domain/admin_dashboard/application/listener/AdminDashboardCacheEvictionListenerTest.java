package com.wanted.backend.domain.admin_dashboard.application.listener;

import com.wanted.backend.domain.admin_dashboard.application.cache.AdminDashboardCache;
import com.wanted.backend.domain.notice.domain.event.NoticeChangedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDashboardCacheEvictionListenerTest {

    private CacheManager cacheManager;
    private Cache cache;
    private MeterRegistry meterRegistry;
    private AdminDashboardCacheEvictionListener listener;

    @BeforeEach
    void setUp() {
        cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        meterRegistry = new SimpleMeterRegistry();
        listener = new AdminDashboardCacheEvictionListener(cacheManager, meterRegistry);
    }

    @Test
    @DisplayName("공지 변경 이벤트를 받으면 대시보드 요약 캐시의 'summary' 키를 무효화한다")
    void evictsSummaryKeyOnNoticeChanged() {
        when(cacheManager.getCache(AdminDashboardCache.CACHE_NAME)).thenReturn(cache);

        listener.onNoticeChanged(NoticeChangedEvent.of(1L, NoticeChangedEvent.ChangeType.CREATED_GLOBAL));

        verify(cache).evict(AdminDashboardCache.SUMMARY_KEY);
    }

    @Test
    @DisplayName("캐시가 등록돼 있지 않으면 조용히 무시한다")
    void doesNothingWhenCacheMissing() {
        when(cacheManager.getCache(AdminDashboardCache.CACHE_NAME)).thenReturn(null);

        assertThatCode(() -> listener.onNoticeChanged(
                NoticeChangedEvent.of(1L, NoticeChangedEvent.ChangeType.UPDATED)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("evict 실패는 예외를 전파하지 않고 실패 메트릭을 증가시킨다(커밋된 공지 작업과 격리)")
    void isolatesEvictFailure() {
        when(cacheManager.getCache(AdminDashboardCache.CACHE_NAME)).thenReturn(cache);
        doThrow(new RuntimeException("Redis down")).when(cache).evict(any());

        assertThatCode(() -> listener.onNoticeChanged(
                NoticeChangedEvent.of(1L, NoticeChangedEvent.ChangeType.DELETED)))
                .doesNotThrowAnyException();

        double failures = meterRegistry.get("admin.dashboard.cache.evict")
                .tag("result", "failure")
                .counter()
                .count();
        assertThat(failures).isEqualTo(1.0);
    }
}
