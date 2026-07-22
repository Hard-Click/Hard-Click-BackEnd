package com.wanted.backend.domain.admin_dashboard.application.listener;

import com.wanted.backend.domain.admin_dashboard.application.cache.AdminDashboardCache;
import com.wanted.backend.domain.notice.domain.event.NoticeChangedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 전체 공지 변경 시 관리자 대시보드 요약 캐시(adminDashboard:v2 / 'summary', TTL 10분)를 무효화한다.
 *
 * 커밋 이후(AFTER_COMMIT) evict하는 이유:
 *  - 커밋 전에 evict하면 커밋~evict 사이에 동시 조회가 커밋 전(구) 데이터로 캐시를 재적재해
 *    TTL이 끝날 때까지 stale이 남을 수 있다.
 *  - evict 실패(예: Redis 장애)를 try/catch로 격리한다. 공지 DB 작업은 이미 커밋됐으므로 여기서
 *    예외를 전파하면 API가 500을 반환하고, 재시도 시 전체 공지가 중복 생성될 수 있다. 실패는
 *    메트릭·로그로만 남기고 삼킨다(다음 변경 또는 TTL 만료 시 자연히 정합성 회복).
 */
@Slf4j
@Component
public class AdminDashboardCacheEvictionListener {

    private final CacheManager cacheManager;
    private final Counter evictSuccesses;
    private final Counter evictFailures;

    public AdminDashboardCacheEvictionListener(CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.cacheManager = cacheManager;
        this.evictSuccesses = Counter.builder("admin.dashboard.cache.evict")
                .tag("result", "success")
                .register(meterRegistry);
        this.evictFailures = Counter.builder("admin.dashboard.cache.evict")
                .tag("result", "failure")
                .register(meterRegistry);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNoticeChanged(NoticeChangedEvent event) {
        try {
            Cache cache = cacheManager.getCache(AdminDashboardCache.CACHE_NAME);
            if (cache != null) {
                cache.evict(AdminDashboardCache.SUMMARY_KEY);
            }
            recordMetric(evictSuccesses);
        } catch (RuntimeException e) {
            recordMetric(evictFailures);
            log.error("[AdminDashboard] 요약 캐시 무효화 실패. noticeId={}, changeType={}",
                    event.noticeId(), event.changeType(), e);
        }
    }

    // AFTER_COMMIT에서는 메트릭 기록 자체의 예외도 커밋 콜러로 새면 안 된다(evict와 동일한 이유).
    // increment는 사실상 예외를 던지지 않지만, 방어적으로 격리한다.
    private void recordMetric(Counter counter) {
        try {
            counter.increment();
        } catch (RuntimeException e) {
            log.error("[AdminDashboard] 캐시 evict 메트릭 기록 실패", e);
        }
    }
}
