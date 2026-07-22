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

    // 오늘 완료는 '당해년(오늘 포함)' 연간뷰와 '해당 월(오늘 포함)' 월간뷰 캐시에만 영향을 준다
    // (과거 연도/월 키는 오늘을 포함하지 않아 무관, 다른 달 월간 키도 무관).
    // GetLessonGrassService.resolveCacheKey 의 현재-연도 키 형식과 일치시켜 그 두 키만 정밀 제거한다.
    //   - 연간: memberId:year:today
    //   - 월간: memberId:year-month:today  (month 는 제로패딩 없음 — resolveCacheKey 와 동일)
    private void evictLessonGrassCache(Long memberId, LocalDate statDate) {
        Cache cache = cacheManager.getCache(LESSON_GRASS_CACHE);
        if (cache == null) {
            return;
        }
        String base = memberId + ":" + statDate.getYear();
        cache.evict(base + ":" + statDate);                                    // 연간뷰 당해년 키
        cache.evict(base + "-" + statDate.getMonthValue() + ":" + statDate);   // 월간뷰 당월 키
    }
}
