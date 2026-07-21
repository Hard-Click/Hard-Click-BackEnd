package com.wanted.backend.domain.study_schedule.application.usecase;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;

import java.time.LocalDate;

public interface ScheduleUseCase {

    /** 기간(from~to) 캘린더 조회. */
    java.util.List<ScheduleDtos.CalendarItem> getMySchedule(Long memberId, LocalDate from, LocalDate to);

    /** 오늘 할 일 + 진행률. */
    ScheduleDtos.TodayView getMyToday(Long memberId, LocalDate today);

    /** 슬롯 완료 체크. */
    void completeSlot(Long memberId, Long slotId);

    /**
     * 계획일이 지난 미완료(PLANNED) 슬롯을 MISSED 로 일괄 전이한다(배치용).
     * @return 전이된 슬롯 수
     */
    int markPastPlannedAsMissed(LocalDate today);
}
