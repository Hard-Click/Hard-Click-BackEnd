package com.wanted.backend.domain.study_schedule.application.service;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.SchedulePlanPort;
import com.wanted.backend.domain.study_schedule.application.port.StudentTodoPort;
import com.wanted.backend.domain.study_schedule.application.usecase.ScheduleUseCase;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ScheduleService implements ScheduleUseCase {

    private static final String STATUS_DONE = "DONE";

    /**
     * 정렬 기준: 날짜 → 시작 시각 → 항목 ID.
     *
     * <p>시작 시각이 없는 항목(AI가 시각을 안 붙였거나 학생이 시간 없이 적은 할 일)은 <b>맨 뒤</b>로 보낸다.
     * 타임테이블에 배치되지 않고 목록에만 보이는 항목이라 시간 있는 것들 사이에 끼면 순서가 어색해진다.
     */
    private static final Comparator<ScheduleDtos.CalendarItem> DISPLAY_ORDER =
            Comparator.comparing(ScheduleDtos.CalendarItem::planDate)
                    .thenComparing(ScheduleDtos.CalendarItem::startTime,
                            Comparator.nullsLast(Comparator.<LocalTime>naturalOrder()))
                    .thenComparing(ScheduleDtos.CalendarItem::itemId,
                            Comparator.nullsLast(Comparator.<Long>naturalOrder()));

    private final SchedulePlanPort schedulePlanPort;
    private final StudentTodoPort studentTodoPort;

    /**
     * AI 슬롯 + 학생 할 일을 합쳐 화면 순서로 정렬한다.
     *
     * <p>합치는 걸 서버가 하는 이유: 진행률(done/total)이 두 소스를 모두 세야 하고,
     * 프론트가 두 번 호출해 직접 병합·정렬하면 정렬 규칙이 화면마다 갈린다.
     */
    private List<ScheduleDtos.CalendarItem> mergedItems(Long memberId, LocalDate from, LocalDate to) {
        return Stream.concat(
                        schedulePlanPort.findSlots(memberId, from, to).stream(),
                        studentTodoPort.findTodos(memberId, from, to).stream())
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleDtos.CalendarItem> getMySchedule(Long memberId, LocalDate from, LocalDate to) {
        return mergedItems(memberId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleDtos.TodayView getMyToday(Long memberId, LocalDate today) {
        List<ScheduleDtos.CalendarItem> items = mergedItems(memberId, today, today);
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
