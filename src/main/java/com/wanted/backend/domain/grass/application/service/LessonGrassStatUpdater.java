package com.wanted.backend.domain.grass.application.service;

import com.wanted.backend.domain.grass.application.port.LessonGrassCountWriter;
import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 레슨 완료(VideoCompletedEvent) 시 수강량 잔디 집계를 갱신한다 — RankingScoreUpdater 의 LESSON 처리와 동일하게
 * 이벤트당 +1. (재완료 중복 방지는 랭킹과 공유하는 별도 크로스컷 이슈 — 소스 이벤트에 가드가 없음)
 *
 * <p>커밋 이후(AFTER_COMMIT) 처리해, 완료 트랜잭션이 롤백되면 집계/캐시 무효화도 일어나지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonGrassStatUpdater {

    private static final String LESSON_GRASS_CACHE = "grassLessons:v3";

    private final LessonGrassCountWriter lessonGrassCountWriter;
    private final CacheManager cacheManager;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VideoCompletedEvent event) {
        try {
            LocalDate statDate = event.occurredAt().atZone(clock.getZone()).toLocalDate();
            lessonGrassCountWriter.increment(event.memberId(), statDate);
            evictLessonGrassCache(event.memberId(), statDate);
        } catch (Exception exception) {
            // 집계/캐시 실패가 완료 처리(이미 커밋됨) 응답에 영향을 주면 안 되므로 로깅만 한다.
            log.error("[LessonGrass] 수강량 집계 갱신 실패. memberId={}, videoId={}",
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
