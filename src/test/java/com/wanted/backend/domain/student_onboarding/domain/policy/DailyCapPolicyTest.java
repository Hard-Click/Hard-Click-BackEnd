package com.wanted.backend.domain.student_onboarding.domain.policy;

import com.wanted.backend.domain.student_onboarding.domain.model.WeeklyAvailability;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class DailyCapPolicyTest {

    private static Set<Integer> allDayBlocked() {
        return IntStream.range(0, 48).boxed().collect(Collectors.toSet());
    }

    /** 종일 비워둬도 하루 24시간을 상한으로 쓰면 안 된다 - 정책 상한으로 잘린다. */
    @Test
    void clampsToPolicyMaximumWhenAvailabilityIsWideOpen() {
        WeeklyAvailability wideOpen = WeeklyAvailability.fromUnavailable(Map.of());

        assertThat(DailyCapPolicy.deriveDailyCapMinutes(wideOpen))
                .isEqualTo(DailyCapPolicy.MAX_DAILY_CAP_MINUTES);
    }

    /** 하루 30분만 가능해도 하한 아래로는 안 내려간다. */
    @Test
    void clampsToPolicyMinimumWhenAvailabilityIsTiny() {
        Set<Integer> allButLastSlot = IntStream.range(0, 47).boxed().collect(Collectors.toSet());
        WeeklyAvailability tiny = WeeklyAvailability.fromUnavailable(
                Map.of(0, allDayBlocked(), 1, allButLastSlot, 2, allDayBlocked(), 3, allDayBlocked(),
                        4, allDayBlocked(), 5, allDayBlocked(), 6, allDayBlocked()));

        assertThat(DailyCapPolicy.deriveDailyCapMinutes(tiny))
                .isEqualTo(DailyCapPolicy.MIN_DAILY_CAP_MINUTES);
    }

    /** 상·하한 사이면 학습일 평균 가용 분이 그대로 상한이 된다. */
    @Test
    void usesStudyDayAverageWhenWithinPolicyBounds() {
        // 월~금 18:00~22:00(4시간=240분) 가능, 주말 종일 불가능
        Set<Integer> blockedExceptEvening = IntStream.range(0, 48).boxed()
                .filter(slot -> slot < 36 || slot >= 44)
                .collect(Collectors.toSet());

        Map<Integer, Set<Integer>> unavailable = new java.util.HashMap<>();
        unavailable.put(0, allDayBlocked());
        unavailable.put(6, allDayBlocked());
        for (int day = 1; day <= 5; day++) {
            unavailable.put(day, blockedExceptEvening);
        }

        WeeklyAvailability availability = WeeklyAvailability.fromUnavailable(unavailable);

        assertThat(availability.studyDayCount()).isEqualTo(5);
        assertThat(availability.totalAvailableMinutes()).isEqualTo(5 * 240);
        assertThat(DailyCapPolicy.deriveDailyCapMinutes(availability)).isEqualTo(240);
    }

    @Test
    void returnsZeroWhenThereIsNoStudyDay() {
        WeeklyAvailability none = WeeklyAvailability.fromUnavailable(
                IntStream.range(0, 7).boxed().collect(Collectors.toMap(d -> d, d -> allDayBlocked())));

        assertThat(DailyCapPolicy.deriveDailyCapMinutes(none)).isZero();
    }
}
