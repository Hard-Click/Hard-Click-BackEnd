package com.wanted.backend.domain.study_schedule.domain.model;

import java.time.LocalTime;

/**
 * 타임테이블(오른쪽 00~23시 그리드) 배치에 쓰는 시작~종료 시각 계산.
 *
 * <p>두 소스가 시간을 다르게 갖는다:
 * <ul>
 *   <li>AI 슬롯: {@code start_time} + {@code planned_min}(영상 강의 길이에서 나온 값) → 종료 시각을 계산해야 한다.
 *   <li>학생 할 일: 사용자가 시작·종료를 직접 적는다 → 계산할 게 없다.
 * </ul>
 */
public final class TimeBlock {

    /** 하루의 마지막 표현 가능 시각. LocalTime/TIME 은 24:00 을 표현할 수 없다. */
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    private TimeBlock() {
    }

    /**
     * 시작 시각과 계획 분으로 종료 시각을 구한다.
     *
     * <p>자정을 넘기면 {@code 23:59:59} 로 자른다. LocalTime 은 순환하므로 그냥 더하면
     * 23:30 + 60분 = 00:30 이 되어 타임테이블에서 <b>하루 맨 위로 올라가 버린다</b>(종료가 시작보다 앞섬).
     * 날짜를 넘는 학습 계획은 현재 모델에 없으므로(슬롯은 plan_date 하루에 속한다) 자르는 게 맞다.
     *
     * @param startTime      시작 시각. null 이면 타임테이블에 배치하지 않는다는 뜻이라 null 반환
     * @param plannedMinutes 계획 학습 시간(분)
     * @return 종료 시각. startTime 이 null 이면 null
     */
    public static LocalTime endOf(LocalTime startTime, int plannedMinutes) {
        if (startTime == null) {
            return null;
        }
        if (plannedMinutes <= 0) {
            return startTime;
        }
        long endSecond = startTime.toSecondOfDay() + (long) plannedMinutes * 60;
        if (endSecond >= END_OF_DAY.toSecondOfDay()) {
            return END_OF_DAY;
        }
        return LocalTime.ofSecondOfDay(endSecond);
    }

    /** 시작~종료 사이 분. 종료가 시작보다 앞서면 0. */
    public static int minutesBetween(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return 0;
        }
        return (endTime.toSecondOfDay() - startTime.toSecondOfDay()) / 60;
    }
}
