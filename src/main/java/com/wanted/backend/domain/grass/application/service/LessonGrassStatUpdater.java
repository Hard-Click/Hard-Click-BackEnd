package com.wanted.backend.domain.grass.application.service;

import com.wanted.backend.domain.grass.application.port.LessonGrassCountWriter;
import com.wanted.backend.domain.learning_activity.application.outbox.VideoCompletionConsumer;
import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import com.wanted.backend.global.idempotency.VideoCompletionDedup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 레슨 완료(VideoCompletedEvent) 시 수강량 잔디 집계를 갱신한다 — 이벤트당 +1.
 *
 * <p>durable outbox relay가 호출하는 멱등 소비자다(즉시 이벤트 리스너 아님). 선점(dedup)과 집계를 <b>한
 * 트랜잭션</b>으로 묶어, 증가 실패 시 선점도 함께 롤백돼 relay 재시도가 안전하다(잔디는 DB라 정확히-1회 보장).
 * 캐시 무효화 실패는 집계를 롤백시키지 않도록 삼킨다(스테일 최소).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonGrassStatUpdater implements VideoCompletionConsumer {

    private static final String LESSON_GRASS_CACHE = "grassLessons:v3";
    private static final String DEDUP_CONSUMER = "lesson_grass";

    private final LessonGrassCountWriter lessonGrassCountWriter;
    private final CacheManager cacheManager;
    private final VideoCompletionDedup videoCompletionDedup;
    private final Clock clock;

    @Override
    public String consumerId() {
        return DEDUP_CONSUMER;
    }

    @Override
    @Transactional
    public void process(VideoCompletedEvent event) {
        // 이미 집계된 완료면 스킵 — 동시 최초완료 경합·이벤트 재전달로 인한 '이벤트당 +1' 중복 집계를 막는다.
        if (!videoCompletionDedup.claim(DEDUP_CONSUMER, event.memberId(), event.videoId())) {
            return;
        }
        LocalDate statDate = event.occurredAt().atZone(clock.getZone()).toLocalDate();
        // 증가 실패는 예외를 그대로 올려 트랜잭션(선점 포함)을 롤백 → relay가 재시도한다.
        lessonGrassCountWriter.increment(event.memberId(), statDate);
        try {
            evictLessonGrassCache(event.memberId(), statDate);
        } catch (Exception exception) {
            // 캐시 무효화 실패로 집계까지 롤백되면 안 된다(캐시는 트랜잭션 밖 자원). 스테일은 조회 시 자연 갱신된다.
            log.warn("[LessonGrass] 캐시 무효화 실패(집계는 반영됨). memberId={}, videoId={}",
                    event.memberId(), event.videoId(), exception);
        }
    }

    // 완료된 날(statDate)이 속한 연간뷰·월간뷰 캐시만 정밀 제거한다.
    // 키 규칙은 조회(resolveCacheKey)와 LessonGrassCacheKey 로 공유한다 — 마지막 세그먼트는 '완료일'이 아니라
    // '조회 시점(오늘=now(clock))'이어야, 자정을 넘겨 처리되는 완료(특히 연말→연초)에서도 실제 조회 키와 어긋나지 않는다.
    private void evictLessonGrassCache(Long memberId, LocalDate statDate) {
        Cache cache = cacheManager.getCache(LESSON_GRASS_CACHE);
        if (cache == null) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        int year = statDate.getYear();
        cache.evict(LessonGrassCacheKey.yearly(memberId, year, today));
        cache.evict(LessonGrassCacheKey.monthly(memberId, year, statDate.getMonthValue(), today));
    }
}
