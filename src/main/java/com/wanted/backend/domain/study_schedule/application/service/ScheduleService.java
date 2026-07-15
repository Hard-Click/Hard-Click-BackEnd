package com.wanted.backend.domain.study_schedule.application.service;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.SchedulePlanPort;
import com.wanted.backend.domain.study_schedule.application.usecase.ScheduleUseCase;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService implements ScheduleUseCase {

    private static final String STATUS_DONE = "DONE";

    private final SchedulePlanPort schedulePlanPort;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleDtos.CalendarItem> getMySchedule(Long memberId, LocalDate from, LocalDate to) {
        return schedulePlanPort.findSlots(memberId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleDtos.TodayView getMyToday(Long memberId, LocalDate today) {
        List<ScheduleDtos.CalendarItem> items = schedulePlanPort.findSlots(memberId, today, today);
        int done = (int) items.stream().filter(item -> STATUS_DONE.equals(item.status())).count();
        return new ScheduleDtos.TodayView(items, done, items.size());
    }

    @Override
    @Transactional
    public void completeSlot(Long memberId, Long slotId) {
        // TODO(reflow 연동): 완료 시 daily_achievement 기록까지 해야 야간/주간 리플로우가 실측을 반영한다.
        //  '오늘 목표 달성' 판정 규칙이 확정되면 이 지점에서 daily_achievement upsert 추가.
        int updated = schedulePlanPort.markSlotDone(memberId, slotId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.SCHEDULE_SLOT_NOT_FOUND);
        }
    }
}
