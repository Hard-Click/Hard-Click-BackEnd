package com.wanted.backend.domain.study_schedule.application.service;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.SchedulePlanPort;
import com.wanted.backend.domain.study_schedule.application.port.StudentTodoPort;
import com.wanted.backend.domain.study_schedule.domain.model.ScheduleItemSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    }
}
