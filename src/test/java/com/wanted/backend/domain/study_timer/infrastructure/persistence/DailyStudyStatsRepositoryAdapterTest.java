package com.wanted.backend.domain.study_timer.infrastructure.persistence;

import com.wanted.backend.domain.study_timer.domain.model.DailyStudyStat;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyStudyStatsRepositoryAdapterTest {

    private SpringDataDailyStudyStatsRepository repository;
    private EntityManager entityManager;
    private DailyStudyStatsRepositoryAdapter adapter;
    private final LocalDate studyDate = LocalDate.parse("2026-05-11");
    private final LocalDateTime now = LocalDateTime.parse("2026-05-11T15:10:00");

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataDailyStudyStatsRepository.class);
        entityManager = mock(EntityManager.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-11T06:10:00Z"), ZoneId.of("Asia/Seoul"));
        adapter = new DailyStudyStatsRepositoryAdapter(repository, entityManager, clock);

        Query mockQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(1);
    }

    @Test
    void 행이_없을때_upsert하면_새_행이_생성된다() {
        DailyStudyStatsJpaEntity created = new DailyStudyStatsJpaEntity(1L, studyDate, 300, now, now);
        when(repository.findByMemberIdAndStatDate(1L, studyDate))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(created));

        DailyStudyStat result = adapter.upsertStudySeconds(1L, studyDate, 300);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.studyDate()).isEqualTo(studyDate);
        assertThat(result.studySeconds()).isEqualTo(300);
        verify(entityManager).createNativeQuery(anyString());
    }

    @Test
    void 행이_이미_있을때_upsert하면_누적값이_증가한다() {
        DailyStudyStatsJpaEntity existing = new DailyStudyStatsJpaEntity(1L, studyDate, 120, now, now);
        DailyStudyStatsJpaEntity updated = new DailyStudyStatsJpaEntity(1L, studyDate, 200, now, now);
        when(repository.findByMemberIdAndStatDate(1L, studyDate))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(updated));

        DailyStudyStat result = adapter.upsertStudySeconds(1L, studyDate, 80);

        assertThat(result.studySeconds()).isEqualTo(200);
        verify(entityManager).createNativeQuery(anyString());
    }

    @Test
    void 오버플로우_발생_시_예외를_던지고_upsert를_실행하지_않는다() {
        DailyStudyStatsJpaEntity entity = new DailyStudyStatsJpaEntity(1L, studyDate, Integer.MAX_VALUE - 10, now, now);
        when(repository.findByMemberIdAndStatDate(1L, studyDate)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> adapter.upsertStudySeconds(1L, studyDate, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDY_TIMER_DAILY_STAT_INVALID);

        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void 기간별_학습_통계를_조회한다() {
        LocalDate startDate = LocalDate.parse("2026-05-01");
        LocalDate endDate = LocalDate.parse("2026-05-03");
        when(repository.findByMemberIdAndStatDateBetweenOrderByStatDateAsc(1L, startDate, endDate))
                .thenReturn(List.of(
                        new DailyStudyStatsJpaEntity(1L, LocalDate.parse("2026-05-01"), 120, now, now),
                        new DailyStudyStatsJpaEntity(1L, LocalDate.parse("2026-05-03"), 300, now, now)
                ));

        List<DailyStudyStat> result = adapter.findByMemberIdAndDateBetween(1L, startDate, endDate);

        assertThat(result)
                .extracting(DailyStudyStat::memberId, DailyStudyStat::studyDate, DailyStudyStat::studySeconds)
                .containsExactly(
                        tuple(1L, LocalDate.parse("2026-05-01"), 120),
                        tuple(1L, LocalDate.parse("2026-05-03"), 300)
                );
    }
}
