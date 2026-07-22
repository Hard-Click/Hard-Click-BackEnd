package com.wanted.backend.domain.grass.application.service;

import com.wanted.backend.domain.grass.application.port.LessonGrassCountWriter;
import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LessonGrassStatUpdaterTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private LessonGrassCountWriter countWriter;
    private CacheManager cacheManager;
    private Cache cache;
    private LessonGrassStatUpdater updater;

    @BeforeEach
    void setUp() {
        countWriter = mock(LessonGrassCountWriter.class);
        cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        when(cacheManager.getCache("grassLessons:v3")).thenReturn(cache);
        // 2026-01-03 00:00 UTC == 2026-01-03 09:00 KST → statDate = 2026-01-03(KST)
        Clock clock = Clock.fixed(Instant.parse("2026-01-03T00:00:00Z"), KST);
        updater = new LessonGrassStatUpdater(countWriter, cacheManager, clock);
    }

    @Test
    void incrementsWatchedLessonCountOnKstDateAndEvictsCache() {
        // 완료 시각(UTC 자정) → KST 날짜 2026-01-03
        VideoCompletedEvent event = new VideoCompletedEvent(
                77L, 55L, 42L, Instant.parse("2026-01-03T00:00:00Z"));

        updater.handle(event);

        verify(countWriter).increment(eq(77L), eq(LocalDate.of(2026, 1, 3)));
        // resolveCacheKey 현재-연도 형식과 동일한 연간·월간 키를 모두 evict
        verify(cache).evict("77:2026:2026-01-03");      // 연간뷰 당해년 키
        verify(cache).evict("77:2026-1:2026-01-03");    // 월간뷰 당월 키(month 제로패딩 없음)
    }

    @Test
    void usesKstDateWhenUtcIsPreviousDay() {
        // UTC 2026-01-02 20:00 == KST 2026-01-03 05:00 → statDate = 2026-01-03(KST)
        VideoCompletedEvent event = new VideoCompletedEvent(
                77L, 55L, 42L, Instant.parse("2026-01-02T20:00:00Z"));

        updater.handle(event);

        verify(countWriter).increment(eq(77L), eq(LocalDate.of(2026, 1, 3)));
        verify(cache).evict("77:2026:2026-01-03");
        verify(cache).evict("77:2026-1:2026-01-03");
    }
}
