package com.wanted.backend.domain.grass.application.service;

import com.wanted.backend.domain.grass.application.port.LessonGrassCountWriter;
import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import com.wanted.backend.global.idempotency.VideoCompletionDedup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LessonGrassStatUpdaterTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private LessonGrassCountWriter countWriter;
    private CacheManager cacheManager;
    private Cache cache;
    private VideoCompletionDedup videoCompletionDedup;
    private LessonGrassStatUpdater updater;

    @BeforeEach
    void setUp() {
        countWriter = mock(LessonGrassCountWriter.class);
        cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        videoCompletionDedup = mock(VideoCompletionDedup.class);
        when(cacheManager.getCache("grassLessons:v3")).thenReturn(cache);
        // 기본: 최초 선점 성공(집계 진행). 중복 스킵 검증 테스트에서만 false로 재정의한다.
        when(videoCompletionDedup.claim(any(), any(), any())).thenReturn(true);
        // 2026-01-03 00:00 UTC == 2026-01-03 09:00 KST → statDate = 2026-01-03(KST)
        Clock clock = Clock.fixed(Instant.parse("2026-01-03T00:00:00Z"), KST);
        updater = new LessonGrassStatUpdater(countWriter, cacheManager, videoCompletionDedup, clock);
    }

    @Test
    void incrementsWatchedLessonCountOnKstDateAndEvictsCache() {
        // 완료 시각(UTC 자정) → KST 날짜 2026-01-03
        VideoCompletedEvent event = new VideoCompletedEvent(
                77L, 55L, 42L, Instant.parse("2026-01-03T00:00:00Z"));

        updater.process(event);

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

        updater.process(event);

        verify(countWriter).increment(eq(77L), eq(LocalDate.of(2026, 1, 3)));
        verify(cache).evict("77:2026:2026-01-03");
        verify(cache).evict("77:2026-1:2026-01-03");
    }

    @Test
    void evictsPastYearKeysWhenCompletionProcessedAfterNewYearMidnight() {
        // 완료는 2025-12-31 23:59(KST) 인데 AFTER_COMMIT 처리는 자정을 넘겨 2026-01-01 00:30(KST)에 일어나는 경우.
        // 조회 시점(now)이 2026 이라 2025 는 '과거 연도' → resolveCacheKey 는 today suffix 없는 고정 키를 쓴다.
        // now = 2026-01-01 00:30 KST
        Clock clock = Clock.fixed(Instant.parse("2025-12-31T15:30:00Z"), KST);
        LessonGrassStatUpdater updaterAfterMidnight =
                new LessonGrassStatUpdater(countWriter, cacheManager, videoCompletionDedup, clock);
        // occurredAt = 2025-12-31 23:59 KST
        VideoCompletedEvent event = new VideoCompletedEvent(
                77L, 55L, 42L, Instant.parse("2025-12-31T14:59:00Z"));

        updaterAfterMidnight.process(event);

        // 집계는 완료일(2025-12-31) 버킷에 +1
        verify(countWriter).increment(eq(77L), eq(LocalDate.of(2025, 12, 31)));
        // 과거 연도 → today suffix 없는 고정 키를 evict (statDate 를 suffix 로 쓰면 이 키를 놓쳐 stale 이 남음)
        verify(cache).evict("77:2025");       // 연간뷰(2025) 과거연도 키
        verify(cache).evict("77:2025-12");    // 월간뷰(2025-12) 과거연도 키
    }

    @Test
    void skipsIncrementAndEvictWhenCompletionAlreadyProcessed() {
        // 이미 처리된 완료 — 선점 실패(dedup). memberId=77, videoId=55
        when(videoCompletionDedup.claim(eq("lesson_grass"), eq(77L), eq(55L))).thenReturn(false);
        VideoCompletedEvent event = new VideoCompletedEvent(
                77L, 55L, 42L, Instant.parse("2026-01-03T00:00:00Z"));

        updater.process(event);

        verifyNoInteractions(countWriter);
        verify(cache, never()).evict(any());
    }
}
