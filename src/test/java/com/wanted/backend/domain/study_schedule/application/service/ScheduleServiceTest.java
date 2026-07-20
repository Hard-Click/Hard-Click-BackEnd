package com.wanted.backend.domain.study_schedule.application.service;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.ReviewPlanPort;
import com.wanted.backend.domain.study_schedule.application.port.SchedulePlanPort;
import com.wanted.backend.domain.study_schedule.application.port.StudentTodoPort;
import com.wanted.backend.domain.study_schedule.domain.model.ScheduleItemSource;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);
    private static final Long MEMBER_ID = 77001L;

    private SchedulePlanPort schedulePlanPort;
    private StudentTodoPort studentTodoPort;
    private ReviewPlanPort reviewPlanPort;
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        schedulePlanPort = mock(SchedulePlanPort.class);
        studentTodoPort = mock(StudentTodoPort.class);
        reviewPlanPort = mock(ReviewPlanPort.class);
        when(reviewPlanPort.findDueReviews(anyLong(), any(), any())).thenReturn(List.of());
        scheduleService = new ScheduleService(schedulePlanPort, studentTodoPort, reviewPlanPort);
    }

    private static ScheduleDtos.CalendarItem lessonAt(long id, LocalTime start, int minutes, String status) {
        return ScheduleDtos.CalendarItem.ofLesson(
                id, TODAY, start, 1063L, 42L, "영어", "수능 영어 실전", 301L, "Unit 3 듣기", minutes, status);
    }

    private static ScheduleDtos.CalendarItem todoAt(long id, LocalTime start, LocalTime end, String status) {
        return ScheduleDtos.CalendarItem.ofTodo(id, TODAY, start, end, "복습", "지난주 복습 퀴즈", status);
    }

    private static ScheduleDtos.CalendarItem reviewOf(long courseId, LocalDate due, String courseTitle) {
        return ScheduleDtos.CalendarItem.ofReview(courseId, due, courseTitle, "PLANNED");
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

    /** 복습 항목은 courseId 를 실은 REVIEW 소스로, 시각이 없어 목록 맨 뒤에 온다(유사퀴즈 진입용). */
    @Test
    void surfacesDueReviewAsReviewSourceWithCourseId() {
        when(schedulePlanPort.findSlots(anyLong(), any(), any())).thenReturn(List.of(
                lessonAt(1L, LocalTime.of(7, 0), 60, "PLANNED")));
        when(studentTodoPort.findTodos(anyLong(), any(), any())).thenReturn(List.of());
        when(reviewPlanPort.findDueReviews(anyLong(), any(), any())).thenReturn(List.of(
                reviewOf(42L, TODAY, "수능 영어 실전")));

        ScheduleDtos.TodayView view = scheduleService.getMyToday(MEMBER_ID, TODAY);

        ScheduleDtos.CalendarItem review = view.items().stream()
                .filter(item -> item.source() == ScheduleItemSource.REVIEW)
                .findFirst().orElseThrow();
        assertThat(review.courseId()).isEqualTo(42L);
        assertThat(review.startTime()).isNull();
        assertThat(review.title()).contains("복습");
        // 시각 없는 REVIEW 는 07:00 강의 뒤에 온다.
        assertThat(view.items()).extracting(ScheduleDtos.CalendarItem::source)
                .containsExactly(ScheduleItemSource.LESSON, ScheduleItemSource.REVIEW);
    }

    /** 복습은 체크박스 완료 대상이 아니므로 진행률 분모(total)에서 빠진다. */
    @Test
    void excludesReviewFromProgressCount() {
        when(schedulePlanPort.findSlots(anyLong(), any(), any())).thenReturn(List.of(
                lessonAt(1L, LocalTime.of(7, 0), 60, "DONE")));
        when(studentTodoPort.findTodos(anyLong(), any(), any())).thenReturn(List.of());
        when(reviewPlanPort.findDueReviews(anyLong(), any(), any())).thenReturn(List.of(
                reviewOf(42L, TODAY, "수능 영어 실전")));

        ScheduleDtos.TodayView view = scheduleService.getMyToday(MEMBER_ID, TODAY);

        assertThat(view.items()).hasSize(2);      // 강의 + 복습이 목록엔 둘 다 보이고
        assertThat(view.doneCount()).isEqualTo(1);
        assertThat(view.totalCount()).isEqualTo(1); // 분모는 완료 가능한 강의 1개만
    }

    /** 오늘 목록은 같은 코스에 여러 날 밀린 복습을 한 줄로 접고, 밀린 것까지 잡도록 from=null 로 조회한다. */
    @Test
    void collapsesMultipleDueDatesPerCourseForToday() {
        when(schedulePlanPort.findSlots(anyLong(), any(), any())).thenReturn(List.of());
        when(studentTodoPort.findTodos(anyLong(), any(), any())).thenReturn(List.of());
        when(reviewPlanPort.findDueReviews(anyLong(), any(), any())).thenReturn(List.of(
                reviewOf(42L, TODAY.minusDays(2), "수능 영어 실전"),
                reviewOf(42L, TODAY.minusDays(1), "수능 영어 실전"),
                reviewOf(50L, TODAY, "수능 수학 실전")));

        ScheduleDtos.TodayView view = scheduleService.getMyToday(MEMBER_ID, TODAY);

        assertThat(view.items()).extracting(ScheduleDtos.CalendarItem::courseId)
                .containsExactlyInAnyOrder(42L, 50L);          // 코스당 한 줄
        assertThat(view.items()).extracting(ScheduleDtos.CalendarItem::planDate)
                .containsOnly(TODAY);                           // 노출 날짜는 오늘로 통일
        verify(reviewPlanPort).findDueReviews(eq(MEMBER_ID), isNull(), eq(TODAY)); // 밀린 것까지(from=null)
    }

    /** 캘린더(기간 조회)는 복습을 접지 않고 예정일 그대로, 요청 기간을 그대로 넘긴다. */
    @Test
    void calendarKeepsReviewOnItsDueDateWithinRange() {
        LocalDate from = TODAY;
        LocalDate to = TODAY.plusDays(6);
        when(schedulePlanPort.findSlots(anyLong(), any(), any())).thenReturn(List.of());
        when(studentTodoPort.findTodos(anyLong(), any(), any())).thenReturn(List.of());
        when(reviewPlanPort.findDueReviews(anyLong(), any(), any())).thenReturn(List.of(
                reviewOf(42L, TODAY.plusDays(3), "수능 영어 실전")));

        List<ScheduleDtos.CalendarItem> items = scheduleService.getMySchedule(MEMBER_ID, from, to);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).planDate()).isEqualTo(TODAY.plusDays(3));
        verify(reviewPlanPort).findDueReviews(MEMBER_ID, from, to); // 캘린더는 from 을 그대로 넘긴다
    }

    @Test
    void completeSlot_succeedsWhenRowUpdated() {
        when(schedulePlanPort.markSlotDone(MEMBER_ID, 5L)).thenReturn(1);

        scheduleService.completeSlot(MEMBER_ID, 5L);

        verify(schedulePlanPort).markSlotDone(MEMBER_ID, 5L);
    }

    @Test
    void completeSlot_throwsWhenNoRowUpdated() {
        when(schedulePlanPort.markSlotDone(MEMBER_ID, 999L)).thenReturn(0);

        assertThatThrownBy(() -> scheduleService.completeSlot(MEMBER_ID, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.SCHEDULE_SLOT_NOT_FOUND.getMessage());
    }

    @Test
    void markPastPlannedAsMissed_delegatesToPortWithTodayAndReturnsCount() {
        when(schedulePlanPort.markMissedBefore(TODAY)).thenReturn(3);

        int updated = scheduleService.markPastPlannedAsMissed(TODAY);

        assertThat(updated).isEqualTo(3);
        verify(schedulePlanPort).markMissedBefore(TODAY);
    }
}
