package com.wanted.backend.domain.study_schedule.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeBlockTest {

    @Test
    void addsPlannedMinutesToStart() {
        // 영어 Unit 3 듣기 07:00 + 60분 → 08:00
        assertThat(TimeBlock.endOf(LocalTime.of(7, 0), 60)).isEqualTo(LocalTime.of(8, 0));
        // 수학 미적분 2강 14:00 + 120분 → 16:00
        assertThat(TimeBlock.endOf(LocalTime.of(14, 0), 120)).isEqualTo(LocalTime.of(16, 0));
    }

    /** 이걸 안 자르면 LocalTime 이 순환해서 종료가 시작보다 앞서고 타임테이블 블록이 뒤집힌다. */
    @Test
    void clampsToEndOfDayInsteadOfWrappingPastMidnight() {
        assertThat(TimeBlock.endOf(LocalTime.of(23, 30), 60)).isEqualTo(LocalTime.of(23, 59, 59));
        assertThat(TimeBlock.endOf(LocalTime.of(22, 0), 600)).isEqualTo(LocalTime.of(23, 59, 59));

        // 정확히 자정에 끝나는 경우도 24:00 이 아니라 잘린 값이어야 한다
        assertThat(TimeBlock.endOf(LocalTime.of(23, 0), 60)).isEqualTo(LocalTime.of(23, 59, 59));
    }

    @Test
    void neverProducesEndBeforeStart() {
        for (int hour = 0; hour < 24; hour++) {
            LocalTime start = LocalTime.of(hour, 0);
            LocalTime end = TimeBlock.endOf(start, 180);
            assertThat(end)
                    .as("%s + 180분 - 종료가 시작보다 앞서면 안 된다", start)
                    .isAfterOrEqualTo(start);
        }
    }

    @Test
    void returnsNullWhenStartIsNull() {
        // 시작 시각 없는 슬롯은 타임테이블에 안 올라간다
        assertThat(TimeBlock.endOf(null, 60)).isNull();
    }

    @Test
    void returnsStartWhenPlannedMinutesIsNotPositive() {
        assertThat(TimeBlock.endOf(LocalTime.of(9, 0), 0)).isEqualTo(LocalTime.of(9, 0));
        assertThat(TimeBlock.endOf(LocalTime.of(9, 0), -30)).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void computesMinutesBetween() {
        assertThat(TimeBlock.minutesBetween(LocalTime.of(20, 0), LocalTime.of(21, 0))).isEqualTo(60);
        assertThat(TimeBlock.minutesBetween(LocalTime.of(14, 0), LocalTime.of(16, 30))).isEqualTo(150);
    }

    @Test
    void minutesBetweenIsZeroForNullOrInvertedRange() {
        assertThat(TimeBlock.minutesBetween(null, LocalTime.of(9, 0))).isZero();
        assertThat(TimeBlock.minutesBetween(LocalTime.of(9, 0), null)).isZero();
        assertThat(TimeBlock.minutesBetween(LocalTime.of(10, 0), LocalTime.of(9, 0))).isZero();
        assertThat(TimeBlock.minutesBetween(LocalTime.of(9, 0), LocalTime.of(9, 0))).isZero();
    }
}
