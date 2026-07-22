package com.wanted.backend.domain.grass.application.service;

import com.wanted.backend.domain.grass.application.query.GetLessonGrassQuery;
import com.wanted.backend.domain.grass.application.usecase.GetLessonGrassUseCase;
import com.wanted.backend.domain.grass.domain.model.LessonGrassStat;
import com.wanted.backend.domain.grass.domain.policy.LessonGrassLevelPolicy;
import com.wanted.backend.domain.grass.domain.policy.MonthlyGrassPeriodPolicy;
import com.wanted.backend.domain.grass.domain.policy.YearlyGrassPeriodPolicy;
import com.wanted.backend.domain.grass.domain.repository.LessonGrassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetLessonGrassServiceTest {

    private LessonGrassRepository repository;
    private GetLessonGrassService service;

    @BeforeEach
    void setUp() {
        repository = mock(LessonGrassRepository.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-01-03T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new GetLessonGrassService(repository, new LessonGrassLevelPolicy(4), new YearlyGrassPeriodPolicy(), new MonthlyGrassPeriodPolicy(), clock);
    }

    @Test
    void returnsYearlyLessonGrassWithZeroFilledFutureDates() {
        LocalDate startDate = LocalDate.parse("2026-01-01");
        LocalDate today = LocalDate.parse("2026-01-03");
        when(repository.findByMemberIdAndDateBetween(1L, startDate, today))
                .thenReturn(List.of(
                        new LessonGrassStat(1L, LocalDate.parse("2026-01-01"), 1),
                        new LessonGrassStat(1L, LocalDate.parse("2026-01-03"), 5)
                ));

        List<GetLessonGrassUseCase.LessonGrassView> result =
                service.handle(new GetLessonGrassQuery(1L, null));

        assertThat(result).hasSize(365);
        assertThat(result.subList(0, 4))
                .extracting(
                        GetLessonGrassUseCase.LessonGrassView::date,
                        GetLessonGrassUseCase.LessonGrassView::watchedLessonCount,
                        GetLessonGrassUseCase.LessonGrassView::level,
                        GetLessonGrassUseCase.LessonGrassView::isFuture
                )
                .containsExactly(
                        tuple(LocalDate.parse("2026-01-01"), 1, 1, false),
                        tuple(LocalDate.parse("2026-01-02"), 0, 0, false),
                        tuple(LocalDate.parse("2026-01-03"), 5, 4, false),
                        tuple(LocalDate.parse("2026-01-04"), 0, 0, true)
                );
        assertThat(result.get(364).date()).isEqualTo(LocalDate.parse("2026-12-31"));
        assertThat(result.get(364).isFuture()).isTrue();

        verify(repository).findByMemberIdAndDateBetween(1L, startDate, today);
    }

    @Test
    void sumsDuplicatedDateRowsDefensively() {
        LocalDate startDate = LocalDate.parse("2026-01-01");
        LocalDate today = LocalDate.parse("2026-01-03");
        when(repository.findByMemberIdAndDateBetween(1L, startDate, today))
                .thenReturn(List.of(
                        new LessonGrassStat(1L, LocalDate.parse("2026-01-02"), 1),
                        new LessonGrassStat(1L, LocalDate.parse("2026-01-02"), 2)
                ));

        List<GetLessonGrassUseCase.LessonGrassView> result =
                service.handle(new GetLessonGrassQuery(1L, null));

        assertThat(result.get(1).watchedLessonCount()).isEqualTo(3);
        assertThat(result.get(1).level()).isEqualTo(3);
        assertThat(result.get(1).isFuture()).isFalse();
    }

    @Test
    void returnsPastYearLessonGrassForFullYearRange() {
        LocalDate startDate = LocalDate.parse("2025-01-01");
        LocalDate endDate = LocalDate.parse("2025-12-31");
        when(repository.findByMemberIdAndDateBetween(1L, startDate, endDate))
                .thenReturn(List.of(
                        new LessonGrassStat(1L, LocalDate.parse("2025-12-31"), 3)
                ));

        List<GetLessonGrassUseCase.LessonGrassView> result =
                service.handle(new GetLessonGrassQuery(1L, 2025));

        assertThat(result).hasSize(365);
        assertThat(result.get(0).date()).isEqualTo(startDate);
        assertThat(result.get(364).date()).isEqualTo(endDate);
        assertThat(result.get(364).watchedLessonCount()).isEqualTo(3);
        assertThat(result).allMatch(view -> !view.isFuture());

        verify(repository).findByMemberIdAndDateBetween(1L, startDate, endDate);
    }

    @Test
    void returnsEmptyFutureYearLessonGrassWithoutQuerying() {
        List<GetLessonGrassUseCase.LessonGrassView> result =
                service.handle(new GetLessonGrassQuery(1L, 2027));

        assertThat(result).hasSize(365);
        assertThat(result).allMatch(view -> view.watchedLessonCount() == 0 && view.isFuture());

        verify(repository, never()).findByMemberIdAndDateBetween(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void returnsOnlyRequestedMonthWhenMonthGiven() {
        // 오늘=2026-01-03, month=1 → 1/1~1/31 만, 조회 범위는 1/1~오늘(1/3)
        when(repository.findByMemberIdAndDateBetween(
                1L, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-03")))
                .thenReturn(List.of(
                        new LessonGrassStat(1L, LocalDate.parse("2026-01-02"), 2)
                ));

        List<GetLessonGrassUseCase.LessonGrassView> result =
                service.handle(new GetLessonGrassQuery(1L, 2026, 1));

        assertThat(result).hasSize(31);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.parse("2026-01-01"));
        assertThat(result.get(30).date()).isEqualTo(LocalDate.parse("2026-01-31"));
        assertThat(result.get(1).watchedLessonCount()).isEqualTo(2);
        assertThat(result.get(1).isFuture()).isFalse();
        assertThat(result.get(30).isFuture()).isTrue();
        verify(repository).findByMemberIdAndDateBetween(
                1L, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-03"));
    }

    @Test
    void rejectsNullMemberId() {
        assertThatThrownBy(() -> service.handle(new GetLessonGrassQuery(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("회원 ID는 필수입니다.");

        verify(repository, never()).findByMemberIdAndDateBetween(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
