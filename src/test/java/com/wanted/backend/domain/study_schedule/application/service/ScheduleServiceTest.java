package com.wanted.backend.domain.study_schedule.application.service;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.SchedulePlanPort;
import com.wanted.backend.domain.study_schedule.application.port.StudentTodoPort;
import com.wanted.backend.domain.study_schedule.domain.model.ScheduleItemSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);
    private static final Long MEMBER_ID = 77001L;

    private SchedulePlanPort schedulePlanPort;
    private StudentTodoPort studentTodoPort;
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        schedulePlanPort = mock(SchedulePlanPort.class);
        studentTodoPort = mock(StudentTodoPort.class);
        scheduleService = new ScheduleService(schedulePlanPort, studentTodoPort);
    }

    private static ScheduleDtos.CalendarItem lessonAt(long id, LocalTime start, int minutes, String status) {
        return ScheduleDtos.CalendarItem.ofLesson(
                id, TODAY, start, 1063L, 42L, "영어", "수능 영어 실전", 301L, "Unit 3 듣기", minutes, status);
    }

    private static ScheduleDtos.CalendarItem todoAt(long id, LocalTime start, LocalTime end, String status) {
        return ScheduleDtos.CalendarItem.ofTodo(id, TODAY, start, end, "복습", "지난주 복습 퀴즈", status);
    }

    @Test
    void mergesAiSlotsAndStudentTodosInTimeOrder() {
        when(schedulePlanPort.findSlots(anyLong(), any(), any())).thenReturn(List.of(
                lessonAt(1L, LocalTime.of(7, 0), 60, "DONE"),
                lessonAt(2L, LocalTime.of(14, 0), 120, "PLANNED")));
        when(studentTodoPort.findTodos(anyLong(), any(), any())).thenReturn(List.of(
                todoAt(10L, LocalTime.of(20, 0), LocalTime.of(21, 0), "PLANNED")));

        List<ScheduleDtos.CalendarItem> items = scheduleService.getMySchedule(MEMBER_ID, TODAY, TODAY);

        assertThat(items).extracting(ScheduleDtos.CalendarItem::startTime)
                .containsExactly(LocalTime.of(7, 0), LocalTime.of(14, 0), LocalTime.of(20, 0));
        assertThat(items).extracting(ScheduleDtos.CalendarItem::source)
                .containsExactly(ScheduleItemSource.LESSON, ScheduleItemSource.LESSON, ScheduleItemSource.TODO);
    }

    /** 타임테이블 블록을 그리려면 종료 시각이 있어야 한다. LESSON 은 계산, TODO 는 입력값. */
    @Test
    void exposesEndTimeForBothSources() {
        when(schedulePlanPort.findSlots(anyLong(), any(), any())).thenReturn(List.of(
                lessonAt(1L, LocalTime.of(7, 0), 60, "PLANNED")));
        when(studentTodoPort.findTodos(anyLong(), any(), any())).thenReturn(List.of(
                todoAt(10L, LocalTime.of(20, 0), LocalTime.of(21, 0), "PLANNED")));

        List<ScheduleDtos.CalendarItem> items = scheduleService.getMySchedule(MEMBER_ID, TODAY, TODAY);

        assertThat(items.get(0).endTime()).isEqualTo(LocalTime.of(8, 0));   // 07:00 + 60분
        assertThat(items.get(1).endTime()).isEqualTo(LocalTime.of(21, 0));  // 사용자 입력
        assertThat(items.get(1).plannedMinutes()).isEqualTo(60);            // 20:00~21:00 에서 유도
    }

    /** 시간 없는 항목이 07:00 앞에 오면 목록 순서가 어색해진다. */
    @Test
    void putsItemsWithoutStartTimeLast() {
        when(schedulePlanPort.findSlots(anyLong(), any(), any())).thenReturn(List.of(
                lessonAt(1L, null, 40, "PLANNED"),
                lessonAt(2L, LocalTime.of(7, 0), 60, "PLANNED")));
        when(studentTodoPort.findTodos(anyLong(), any(), any())).thenReturn(List.of(
                todoAt(10L, null, null, "PLANNED")));

        List<ScheduleDtos.CalendarItem> items = scheduleService.getMySchedule(MEMBER_ID, TODAY, TODAY);

        assertThat(items.get(0).startTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(items).extracting(ScheduleDtos.CalendarItem::startTime).containsExactly(
                LocalTime.of(7, 0), null, null);
    }

    /** 진행률은 AI 강의와 학생 할 일을 함께 센다 — 스샷의 1/3 이 이 계산이다. */
    @Test
    void countsProgressAcrossBothSources() {
        when(schedulePlanPort.findSlots(anyLong(), any(), any())).thenReturn(List.of(
                lessonAt(1L, LocalTime.of(7, 0), 60, "DONE"),
                lessonAt(2L, LocalTime.of(14, 0), 120, "PLANNED")));
        when(studentTodoPort.findTodos(anyLong(), any(), any())).thenReturn(List.of(
                todoAt(10L, LocalTime.of(20, 0), LocalTime.of(21, 0), "PLANNED")));

        ScheduleDtos.TodayView view = scheduleService.getMyToday(MEMBER_ID, TODAY);

        assertThat(view.doneCount()).isEqualTo(1);
        assertThat(view.totalCount()).isEqualTo(3);
    }

    @Test
    void countsCompletedStudentTodoTowardProgress() {
        when(schedulePlanPort.findSlots(anyLong(), any(), any())).thenReturn(List.of(
                lessonAt(1L, LocalTime.of(7, 0), 60, "PLANNED")));
        when(studentTodoPort.findTodos(anyLong(), any(), any())).thenReturn(List.of(
                todoAt(10L, LocalTime.of(20, 0), LocalTime.of(21, 0), "DONE")));

        ScheduleDtos.TodayView view = scheduleService.getMyToday(MEMBER_ID, TODAY);

        assertThat(view.doneCount()).isEqualTo(1);
        assertThat(view.totalCount()).isEqualTo(2);
    }

    @Test
    void returnsOnlyTodosWhenStudentHasNoAiPlanYet() {
        when(schedulePlanPort.findSlots(anyLong(), any(), any())).thenReturn(List.of());
        when(studentTodoPort.findTodos(anyLong(), any(), any())).thenReturn(List.of(
                todoAt(10L, LocalTime.of(20, 0), LocalTime.of(21, 0), "PLANNED")));

        ScheduleDtos.TodayView view = scheduleService.getMyToday(MEMBER_ID, TODAY);

        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).source()).isEqualTo(ScheduleItemSource.TODO);
        assertThat(view.items().get(0).enrollmentId()).isNull();
        assertThat(view.totalCount()).isEqualTo(1);
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
