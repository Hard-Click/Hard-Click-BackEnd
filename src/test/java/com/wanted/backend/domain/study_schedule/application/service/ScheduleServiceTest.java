package com.wanted.backend.domain.study_schedule.application.service;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.SchedulePlanPort;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @InjectMocks
    private ScheduleService scheduleService;

    @Mock
    private SchedulePlanPort schedulePlanPort;

    private ScheduleDtos.CalendarItem slot(Long slotId, String status) {
        return new ScheduleDtos.CalendarItem(
                slotId,
                LocalDate.of(2026, 7, 15),
                LocalTime.of(9, 0),
                100L,
                200L,
                "수학",
                "미적분",
                300L,
                "1강",
                60,
                status);
    }

    @Test
    @DisplayName("기간 캘린더 조회는 포트 결과를 그대로 반환한다")
    void getMySchedule_delegates() {
        // given
        LocalDate from = LocalDate.of(2026, 7, 13);
        LocalDate to = LocalDate.of(2026, 7, 19);
        List<ScheduleDtos.CalendarItem> slots = List.of(slot(1L, "PENDING"), slot(2L, "DONE"));
        given(schedulePlanPort.findSlots(100L, from, to)).willReturn(slots);

        // when
        List<ScheduleDtos.CalendarItem> result = scheduleService.getMySchedule(100L, from, to);

        // then
        assertThat(result).isSameAs(slots);
        verify(schedulePlanPort).findSlots(100L, from, to);
    }

    @Test
    @DisplayName("오늘 할 일 조회는 DONE 개수와 전체 개수를 집계한다")
    void getMyToday_countsDoneAndTotal() {
        // given
        LocalDate today = LocalDate.of(2026, 7, 15);
        given(schedulePlanPort.findSlots(100L, today, today)).willReturn(
                List.of(slot(1L, "DONE"), slot(2L, "PENDING"), slot(3L, "DONE")));

        // when
        ScheduleDtos.TodayView view = scheduleService.getMyToday(100L, today);

        // then
        assertThat(view.totalCount()).isEqualTo(3);
        assertThat(view.doneCount()).isEqualTo(2);
        assertThat(view.items()).hasSize(3);
    }

    @Test
    @DisplayName("오늘 슬롯이 없으면 진행률은 0/0이다")
    void getMyToday_emptyReturnsZeroProgress() {
        // given
        LocalDate today = LocalDate.of(2026, 7, 15);
        given(schedulePlanPort.findSlots(100L, today, today)).willReturn(List.of());

        // when
        ScheduleDtos.TodayView view = scheduleService.getMyToday(100L, today);

        // then
        assertThat(view.totalCount()).isZero();
        assertThat(view.doneCount()).isZero();
        assertThat(view.items()).isEmpty();
    }

    @Test
    @DisplayName("슬롯 완료 처리 시 갱신 행이 있으면 예외 없이 완료된다")
    void completeSlot_success() {
        // given
        given(schedulePlanPort.markSlotDone(100L, 5L)).willReturn(1);

        // when
        scheduleService.completeSlot(100L, 5L);

        // then
        verify(schedulePlanPort).markSlotDone(100L, 5L);
    }

    @Test
    @DisplayName("갱신 행이 0이면(없음/타인 소유) 슬롯 완료 처리에서 예외가 발생한다")
    void completeSlot_fail_whenNoRowUpdated() {
        // given
        given(schedulePlanPort.markSlotDone(100L, 999L)).willReturn(0);

        // when & then
        assertThatThrownBy(() -> scheduleService.completeSlot(100L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SCHEDULE_SLOT_NOT_FOUND.getMessage());
    }
}
