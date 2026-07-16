package com.wanted.backend.domain.student_onboarding.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyAvailabilityTest {

    /** 요일 전체(0~47)를 불가능으로 채운다. */
    private static Set<Integer> allDayBlocked() {
        return IntStream.range(0, 48).boxed().collect(Collectors.toSet());
    }

    @Test
    void invertsUnavailableSlotsIntoOneContiguousBlock() {
        // 월요일 00:00~08:00(슬롯 0~15) 불가능 -> 08:00 이후만 가능
        WeeklyAvailability availability = WeeklyAvailability.fromUnavailable(
                Map.of(1, IntStream.range(0, 16).boxed().collect(Collectors.toSet())));

        List<WeeklyAvailability.AvailabilityBlock> monday = availability.toBlocks().stream()
                .filter(b -> b.dayOfWeek() == 1)
                .toList();

        assertThat(monday).hasSize(1);
        assertThat(monday.get(0).startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(monday.get(0).endTime()).isEqualTo(LocalTime.of(23, 59, 59));
    }

    @Test
    void splitsIntoTwoBlocksWhenUnavailableIsInTheMiddle() {
        // 화요일 10:00~11:00(슬롯 20~21)만 불가능 -> 앞뒤 두 구간으로 쪼개진다
        WeeklyAvailability availability = WeeklyAvailability.fromUnavailable(Map.of(2, Set.of(20, 21)));

        List<WeeklyAvailability.AvailabilityBlock> tuesday = availability.toBlocks().stream()
                .filter(b -> b.dayOfWeek() == 2)
                .toList();

        assertThat(tuesday).hasSize(2);
        assertThat(tuesday.get(0).startTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(tuesday.get(0).endTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(tuesday.get(1).startTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(tuesday.get(1).endTime()).isEqualTo(LocalTime.of(23, 59, 59));
    }

    @Test
    void treatsFullyBlockedDayAsRestDayWithNoBlocks() {
        // 일요일(0)·토요일(6) 종일 불가능 -> bit0 + bit6 = 1 + 64 = 65
        WeeklyAvailability availability = WeeklyAvailability.fromUnavailable(
                Map.of(0, allDayBlocked(), 6, allDayBlocked()));

        assertThat(availability.toRestDaysBitmask()).isEqualTo(65);
        assertThat(availability.studyDayCount()).isEqualTo(5);
        assertThat(availability.toBlocks())
                .noneMatch(b -> b.dayOfWeek() == 0 || b.dayOfWeek() == 6);
    }

    @Test
    void treatsUnlistedDayAsFullyAvailable() {
        // 아무 요일도 안 보내면 7일 종일 가능 = 쉬는 날 없음
        WeeklyAvailability availability = WeeklyAvailability.fromUnavailable(Map.of());

        assertThat(availability.toRestDaysBitmask()).isZero();
        assertThat(availability.studyDayCount()).isEqualTo(7);
        assertThat(availability.toBlocks()).hasSize(7);
        assertThat(availability.totalAvailableMinutes()).isEqualTo(7 * 24 * 60);
    }

    @Test
    void countsMinutesBySlotSoMidnightBlockIsNotShortOneSecond() {
        // 23:30~24:00(슬롯 47)만 가능. end_time 은 23:59:59 로 저장되지만 총량은 슬롯 기준 30분이어야 한다.
        Set<Integer> blocked = IntStream.range(0, 47).boxed().collect(Collectors.toSet());
        WeeklyAvailability availability = WeeklyAvailability.fromUnavailable(
                Map.of(0, allDayBlocked(), 1, blocked, 2, allDayBlocked(), 3, allDayBlocked(),
                        4, allDayBlocked(), 5, allDayBlocked(), 6, allDayBlocked()));

        assertThat(availability.totalAvailableMinutes()).isEqualTo(30);
        assertThat(availability.studyDayCount()).isEqualTo(1);
        assertThat(availability.averageAvailableMinutesPerStudyDay()).isEqualTo(30);
    }

    @Test
    void averagesOverStudyDaysOnlyExcludingRestDays() {
        // 월요일만 12시간(24슬롯) 가능, 나머지 6일은 종일 불가능 -> 평균은 12시간(720분), 7로 나누지 않는다
        Set<Integer> halfDayBlocked = IntStream.range(0, 24).boxed().collect(Collectors.toSet());
        WeeklyAvailability availability = WeeklyAvailability.fromUnavailable(
                Map.of(0, allDayBlocked(), 1, halfDayBlocked, 2, allDayBlocked(), 3, allDayBlocked(),
                        4, allDayBlocked(), 5, allDayBlocked(), 6, allDayBlocked()));

        assertThat(availability.studyDayCount()).isEqualTo(1);
        assertThat(availability.totalAvailableMinutes()).isEqualTo(720);
        assertThat(availability.averageAvailableMinutesPerStudyDay()).isEqualTo(720);
    }

    @Test
    void reportsNoStudyDaysWhenEveryDayIsBlocked() {
        WeeklyAvailability availability = WeeklyAvailability.fromUnavailable(
                IntStream.range(0, 7).boxed()
                        .collect(Collectors.toMap(d -> d, d -> allDayBlocked())));

        assertThat(availability.studyDayCount()).isZero();
        assertThat(availability.toBlocks()).isEmpty();
        assertThat(availability.toRestDaysBitmask()).isEqualTo(0b1111111);
        assertThat(availability.averageAvailableMinutesPerStudyDay()).isZero();
    }
}
