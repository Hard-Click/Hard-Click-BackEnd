package com.wanted.backend.domain.ranking.application.service;

import com.wanted.backend.domain.community.domain.event.CommentAcceptedEvent;
import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import com.wanted.backend.domain.ranking.application.port.RankingScoreWriter;
import com.wanted.backend.domain.ranking.domain.model.RankingMetric;
import com.wanted.backend.domain.ranking.domain.model.RankingPeriod;
import com.wanted.backend.domain.study_timer.domain.event.StudySessionEndedEvent;
import com.wanted.backend.global.idempotency.VideoCompletionDedup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RankingScoreUpdaterTest {

    private RankingScoreWriter rankingScoreWriter;
    private VideoCompletionDedup videoCompletionDedup;
    private RankingScoreUpdater updater;

    @BeforeEach
    void setUp() {
        rankingScoreWriter = mock(RankingScoreWriter.class);
        videoCompletionDedup = mock(VideoCompletionDedup.class);
        // 기본: 최초 선점 성공(집계 진행). 중복 스킵 검증 테스트에서만 false로 재정의한다.
        when(videoCompletionDedup.claim(any(), any(), any())).thenReturn(true);
        updater = new RankingScoreUpdater(rankingScoreWriter, videoCompletionDedup);
    }

    @Test
    void incrementsStudyTimeScoresForAllPeriods() {
        StudySessionEndedEvent event = StudySessionEndedEvent.of(
                1L,
                LocalDate.parse("2026-05-11"),
                300,
                OffsetDateTime.parse("2026-05-11T15:05:00+09:00")
        );

        updater.handle(event);

        verify(rankingScoreWriter).incrementScore(RankingMetric.STUDY_TIME, RankingPeriod.DAILY, 1L, 300);
        verify(rankingScoreWriter).incrementScore(RankingMetric.STUDY_TIME, RankingPeriod.WEEKLY, 1L, 300);
        verify(rankingScoreWriter).incrementScore(RankingMetric.STUDY_TIME, RankingPeriod.MONTHLY, 1L, 300);
    }

    @Test
    void keepsUpdatingOtherPeriodsWhenRedisUpdateFails() {
        StudySessionEndedEvent event = StudySessionEndedEvent.of(
                1L,
                LocalDate.parse("2026-05-11"),
                300,
                OffsetDateTime.parse("2026-05-11T15:05:00+09:00")
        );
        doThrow(new RuntimeException("redis down"))
                .when(rankingScoreWriter)
                .incrementScore(RankingMetric.STUDY_TIME, RankingPeriod.DAILY, 1L, 300);

        updater.handle(event);

        verify(rankingScoreWriter).incrementScore(RankingMetric.STUDY_TIME, RankingPeriod.DAILY, 1L, 300);
        verify(rankingScoreWriter).incrementScore(RankingMetric.STUDY_TIME, RankingPeriod.WEEKLY, 1L, 300);
        verify(rankingScoreWriter).incrementScore(RankingMetric.STUDY_TIME, RankingPeriod.MONTHLY, 1L, 300);
    }

    @Test
    void ignoresNonPositiveDelta() {
        StudySessionEndedEvent event = StudySessionEndedEvent.of(
                1L,
                LocalDate.parse("2026-05-11"),
                0,
                OffsetDateTime.parse("2026-05-11T15:05:00+09:00")
        );

        updater.handle(event);

        verifyNoInteractions(rankingScoreWriter);
    }

    @Test
    void incrementsAcceptedCommentScoresForAllPeriods() {
        CommentAcceptedEvent event = CommentAcceptedEvent.of(1L, 50L, 100L);

        updater.handle(event);

        verify(rankingScoreWriter).incrementScore(RankingMetric.ACCEPTED_COMMENT, RankingPeriod.DAILY, 1L, 1L);
        verify(rankingScoreWriter).incrementScore(RankingMetric.ACCEPTED_COMMENT, RankingPeriod.WEEKLY, 1L, 1L);
        verify(rankingScoreWriter).incrementScore(RankingMetric.ACCEPTED_COMMENT, RankingPeriod.MONTHLY, 1L, 1L);
    }

    @Test
    void keepsUpdatingOtherPeriodsWhenAcceptedCommentRedisUpdateFails() {
        CommentAcceptedEvent event = CommentAcceptedEvent.of(1L, 50L, 100L);
        doThrow(new RuntimeException("redis down"))
                .when(rankingScoreWriter)
                .incrementScore(RankingMetric.ACCEPTED_COMMENT, RankingPeriod.DAILY, 1L, 1L);

        updater.handle(event);

        verify(rankingScoreWriter).incrementScore(RankingMetric.ACCEPTED_COMMENT, RankingPeriod.DAILY, 1L, 1L);
        verify(rankingScoreWriter).incrementScore(RankingMetric.ACCEPTED_COMMENT, RankingPeriod.WEEKLY, 1L, 1L);
        verify(rankingScoreWriter).incrementScore(RankingMetric.ACCEPTED_COMMENT, RankingPeriod.MONTHLY, 1L, 1L);
    }

    @Test
    void incrementsLessonScoresForAllPeriods() {
        VideoCompletedEvent event = VideoCompletedEvent.of(1L, 10L, 20L);

        updater.process(event);

        verify(rankingScoreWriter).incrementScore(RankingMetric.LESSON, RankingPeriod.DAILY, 1L, 1L);
        verify(rankingScoreWriter).incrementScore(RankingMetric.LESSON, RankingPeriod.WEEKLY, 1L, 1L);
        verify(rankingScoreWriter).incrementScore(RankingMetric.LESSON, RankingPeriod.MONTHLY, 1L, 1L);
    }

    @Test
    void keepsUpdatingOtherPeriodsWhenLessonRedisUpdateFails() {
        VideoCompletedEvent event = VideoCompletedEvent.of(1L, 10L, 20L);
        doThrow(new RuntimeException("redis down"))
                .when(rankingScoreWriter)
                .incrementScore(RankingMetric.LESSON, RankingPeriod.DAILY, 1L, 1L);

        updater.process(event);

        verify(rankingScoreWriter).incrementScore(RankingMetric.LESSON, RankingPeriod.DAILY, 1L, 1L);
        verify(rankingScoreWriter).incrementScore(RankingMetric.LESSON, RankingPeriod.WEEKLY, 1L, 1L);
        verify(rankingScoreWriter).incrementScore(RankingMetric.LESSON, RankingPeriod.MONTHLY, 1L, 1L);
    }

    @Test
    void skipsLessonScoreWhenCompletionAlreadyProcessed() {
        VideoCompletedEvent event = VideoCompletedEvent.of(1L, 10L, 20L);
        // 이미 처리된 완료 — 선점 실패(dedup)
        when(videoCompletionDedup.claim(eq("ranking_lesson"), eq(1L), eq(10L))).thenReturn(false);

        updater.process(event);

        verifyNoInteractions(rankingScoreWriter);
    }

    @Test
    void ignoresNullDelta() {
        StudySessionEndedEvent event = StudySessionEndedEvent.of(
                1L,
                LocalDate.parse("2026-05-11"),
                null,
                OffsetDateTime.parse("2026-05-11T15:05:00+09:00")
        );

        updater.handle(event);

        verifyNoInteractions(rankingScoreWriter);
    }
}
