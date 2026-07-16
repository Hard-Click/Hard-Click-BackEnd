package com.wanted.backend.domain.student_onboarding.domain.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 온보딩 2단계(불가능한 시간 체크)의 주간 가용시간.
 *
 * <p>화면은 30분 단위 주간 그리드에서 <b>불가능한</b> 칸을 받는다. 반면 student_availability 는
 * <b>가능한</b> 구간(start_time~end_time)을 저장하므로 여기서 여집합을 취해 연속 구간으로 병합한다.
 *
 * <p>요일 0=일 ... 6=토 (student_availability.day_of_week, enrollment_onboarding.rest_days 비트마스크와 동일 규약).
 * 슬롯 인덱스 0~47, 슬롯 i = 00:00 기준 i*30분 ~ (i+1)*30분.
 */
public final class WeeklyAvailability {

    public static final int SLOT_MINUTES = 30;
    public static final int SLOTS_PER_DAY = 48;
    public static final int DAYS_PER_WEEK = 7;

    /** 요일별 가용 슬롯 인덱스 집합(불가능의 여집합). */
    private final Map<Integer, Set<Integer>> availableSlotsByDay;

    private WeeklyAvailability(Map<Integer, Set<Integer>> availableSlotsByDay) {
        this.availableSlotsByDay = availableSlotsByDay;
    }

    /**
     * 불가능 슬롯으로부터 생성한다.
     *
     * @param unavailableSlotsByDay 요일(0~6) -> 불가능 슬롯 인덱스(0~47). 빈 요일은 종일 가능으로 본다.
     */
    public static WeeklyAvailability fromUnavailable(Map<Integer, Set<Integer>> unavailableSlotsByDay) {
        Map<Integer, Set<Integer>> available = new java.util.HashMap<>();
        for (int day = 0; day < DAYS_PER_WEEK; day++) {
            Set<Integer> blocked = unavailableSlotsByDay.getOrDefault(day, Set.of());
            Set<Integer> free = new java.util.TreeSet<>();
            for (int slot = 0; slot < SLOTS_PER_DAY; slot++) {
                if (!blocked.contains(slot)) {
                    free.add(slot);
                }
            }
            available.put(day, free);
        }
        return new WeeklyAvailability(available);
    }

    /**
     * 가용 슬롯을 연속 구간으로 병합해 반환한다. student_availability 행과 1:1 대응한다.
     *
     * <p>자정에 끝나는 구간의 end_time 은 {@code 23:59:59}로 저장한다 - JPA 의 LocalTime/TIME 이
     * 24:00 을 표현할 수 없기 때문이다. 가용시간 총량 계산은 시각이 아니라 슬롯 수로 하므로(
     * {@link #totalAvailableMinutes()}) 이 1초 차이가 용량 계산에 섞이지 않는다.
     */
    public List<AvailabilityBlock> toBlocks() {
        List<AvailabilityBlock> blocks = new ArrayList<>();
        for (int day = 0; day < DAYS_PER_WEEK; day++) {
            Set<Integer> free = availableSlotsByDay.getOrDefault(day, Set.of());
            Integer blockStart = null;
            Integer previous = null;

            for (int slot = 0; slot <= SLOTS_PER_DAY; slot++) {
                boolean isFree = slot < SLOTS_PER_DAY && free.contains(slot);
                if (isFree && blockStart == null) {
                    blockStart = slot;
                } else if (!isFree && blockStart != null) {
                    blocks.add(new AvailabilityBlock(day, startTimeOf(blockStart), endTimeOf(previous)));
                    blockStart = null;
                }
                if (isFree) {
                    previous = slot;
                }
            }
        }
        return blocks;
    }

    /**
     * 휴식 요일 비트마스크. 종일 불가능한 요일 = 쉬는 날. (bit0=일 ... bit6=토)
     */
    public int toRestDaysBitmask() {
        int mask = 0;
        for (int day = 0; day < DAYS_PER_WEEK; day++) {
            if (availableSlotsByDay.getOrDefault(day, Set.of()).isEmpty()) {
                mask |= (1 << day);
            }
        }
        return mask;
    }

    /** 학습 가능한 요일 수(쉬는 날 제외). 0 이면 온보딩 입력이 잘못된 것이다. */
    public int studyDayCount() {
        return (int) availableSlotsByDay.values().stream().filter(s -> !s.isEmpty()).count();
    }

    /** 주간 총 가용 분. 슬롯 수 기반이라 정확하다. */
    public int totalAvailableMinutes() {
        return availableSlotsByDay.values().stream().mapToInt(Set::size).sum() * SLOT_MINUTES;
    }

    /** 학습 가능한 요일들의 하루 평균 가용 분. 쉬는 날은 분모에서 뺀다. */
    public int averageAvailableMinutesPerStudyDay() {
        int studyDays = studyDayCount();
        if (studyDays == 0) {
            return 0;
        }
        return totalAvailableMinutes() / studyDays;
    }

    private static LocalTime startTimeOf(int slot) {
        return LocalTime.ofSecondOfDay((long) slot * SLOT_MINUTES * 60);
    }

    private static LocalTime endTimeOf(int slot) {
        int endMinutes = (slot + 1) * SLOT_MINUTES;
        if (endMinutes >= 24 * 60) {
            return LocalTime.of(23, 59, 59);
        }
        return LocalTime.ofSecondOfDay((long) endMinutes * 60);
    }

    /** 가용 구간 하나 - student_availability 한 행. */
    public record AvailabilityBlock(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
    }
}
