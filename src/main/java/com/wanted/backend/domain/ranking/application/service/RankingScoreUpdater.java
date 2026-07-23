package com.wanted.backend.domain.ranking.application.service;

import com.wanted.backend.domain.community.domain.event.CommentAcceptedEvent;
import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import com.wanted.backend.domain.ranking.application.port.RankingScoreWriter;
import com.wanted.backend.domain.ranking.domain.model.RankingMetric;
import com.wanted.backend.domain.ranking.domain.model.RankingPeriod;
import com.wanted.backend.domain.learning_activity.application.outbox.VideoCompletionConsumer;
import com.wanted.backend.domain.study_timer.domain.event.StudySessionEndedEvent;
import com.wanted.backend.global.idempotency.VideoCompletionDedup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScoreUpdater implements VideoCompletionConsumer {

    private static final String DEDUP_CONSUMER = "ranking_lesson";

    private final RankingScoreWriter rankingScoreWriter;
    private final VideoCompletionDedup videoCompletionDedup;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StudySessionEndedEvent event) {
        if (event.deltaStudySeconds() == null || event.deltaStudySeconds() <= 0) {
            return;
        }

        for (RankingPeriod period : RankingPeriod.values()) {
            incrementStudyTimeScore(event, period);
        }
    }

    @Override
    public String consumerId() {
        return DEDUP_CONSUMER;
    }

    /**
     * durable outbox relay가 호출한다(즉시 이벤트 리스너 아님). 멱등 선점으로 중복 집계를 막는다.
     *
     * <p>랭킹은 Redis라 DB 선점과 원자적으로 묶을 수 없어 best-effort다: 선점 커밋 후 일부 기간 증가가 실패해도
     * 예외를 던지지 않는다(재시도해도 dedup에 막혀 소용없음 — Redis INCR은 멱등이 아님). outbox의 이득은
     * '크래시로 소비 자체가 누락되던' 경우를 relay 재시도로 없애는 것이다. (정확히-1회는 잔디(DB)에서만 보장.)
     */
    @Override
    public void process(VideoCompletedEvent event) {
        if (!videoCompletionDedup.claim(DEDUP_CONSUMER, event.memberId(), event.videoId())) {
            return;
        }

        for (RankingPeriod period : RankingPeriod.values()) {
            try {
                rankingScoreWriter.incrementScore(
                        RankingMetric.LESSON,
                        period,
                        event.memberId(),
                        1L
                );
            } catch (Exception exception) {
                log.error(
                        "[Ranking] lesson score increment failed. memberId={}, period={}, videoId={}",
                        event.memberId(),
                        period.value(),
                        event.videoId(),
                        exception
                );
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CommentAcceptedEvent event) {
        for (RankingPeriod period : RankingPeriod.values()) {
            try {
                rankingScoreWriter.incrementScore(RankingMetric.ACCEPTED_COMMENT, period, event.commentAuthorId(), 1L);
            } catch (Exception exception) {
                log.error(
                        "[Ranking] accepted-comment score increment failed. memberId={}, period={}, commentId={}",
                        event.commentAuthorId(),
                        period.value(),
                        event.commentId(),
                        exception
                );
            }
        }
    }

    private void incrementStudyTimeScore(StudySessionEndedEvent event, RankingPeriod period) {
        try {
            rankingScoreWriter.incrementScore(
                    RankingMetric.STUDY_TIME,
                    period,
                    event.memberId(),
                    event.deltaStudySeconds()
            );
        } catch (Exception exception) {
            log.error(
                    "[Ranking] study-time score increment failed. memberId={}, period={}, deltaStudySeconds={}",
                    event.memberId(),
                    period.value(),
                    event.deltaStudySeconds(),
                    exception
            );
        }
    }
}
