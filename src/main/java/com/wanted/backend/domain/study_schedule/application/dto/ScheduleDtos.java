package com.wanted.backend.domain.study_schedule.application.dto;

import com.wanted.backend.domain.study_schedule.domain.model.ScheduleItemSource;
import com.wanted.backend.domain.study_schedule.domain.model.TimeBlock;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 학습 스케줄 조회 결과 DTO(application).
 *
 * <p>화면은 두 소스를 합쳐서 보여준다:
 * <ul>
 *   <li><b>AI 슬롯</b> - AI가 선계산해 weekly_schedule/schedule_slot 에 저장한 활성 계획.
 *       학생은 이걸 편집하지 않고 완료 체크만 한다(소유 모델 유지).
 *   <li><b>학생 할 일</b> - 학생이 '+ 할 일 추가'로 직접 넣은 항목(student_todo). 학생이 CRUD 한다.
 * </ul>
 */
public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    /**
     * 캘린더 한 칸 / 오늘 할 일 한 줄 / 타임테이블 블록 하나.
     *
     * @param source         LESSON(AI 슬롯) | TODO(학생 할 일). 완료 처리 엔드포인트가 갈린다
     * @param itemId         source=LESSON 이면 schedule_slot.id, TODO 면 student_todo.id
     * @param startTime      없을 수 있다. null 이면 타임테이블에 배치하지 않고 목록에만 노출
     * @param endTime        타임테이블 블록 높이용. startTime 이 null 이면 null
     * @param subject        캘린더 색상 구분용
     * @param title          화면에 노출할 제목(LESSON=레슨명, TODO=사용자 입력 제목)
     * @param plannedMinutes 계획 학습 시간(분)
     * @param enrollmentId   source=TODO 면 null
     * @param courseId       source=TODO 면 null
     * @param courseTitle    source=TODO 면 null
     * @param lessonId       source=TODO 면 null
     * @param lessonTitle    source=TODO 면 null
     */
    public record CalendarItem(
            ScheduleItemSource source,
            Long itemId,
            LocalDate planDate,
            LocalTime startTime,
            LocalTime endTime,
            String subject,
            String title,
            int plannedMinutes,
            String status,
            Long enrollmentId,
            Long courseId,
            String courseTitle,
            Long lessonId,
            String lessonTitle
    ) {

        /** AI 슬롯 항목. 종료 시각은 시작 + 계획분(영상 강의 길이에서 나온 값)으로 계산한다. */
        public static CalendarItem ofLesson(
                Long slotId, LocalDate planDate, LocalTime startTime,
                Long enrollmentId, Long courseId, String subject, String courseTitle,
                Long lessonId, String lessonTitle, int plannedMinutes, String status) {
            return new CalendarItem(
                    ScheduleItemSource.LESSON, slotId, planDate, startTime,
                    TimeBlock.endOf(startTime, plannedMinutes),
                    subject, lessonTitle, plannedMinutes, status,
                    enrollmentId, courseId, courseTitle, lessonId, lessonTitle);
        }

        /** 학생 할 일 항목. 시작·종료는 사용자가 직접 적은 값을 그대로 쓴다. */
        public static CalendarItem ofTodo(
                Long todoId, LocalDate planDate, LocalTime startTime, LocalTime endTime,
                String subject, String title, String status) {
            return new CalendarItem(
                    ScheduleItemSource.TODO, todoId, planDate, startTime, endTime,
                    subject, title, TimeBlock.minutesBetween(startTime, endTime), status,
                    null, null, null, null, null);
        }
    }

    /** '오늘 할 일' + 진행률(done/total). AI 슬롯과 학생 할 일을 합쳐서 센다. */
    public record TodayView(
            List<CalendarItem> items,
            int doneCount,
            int totalCount
    ) {
    }

    /** 할 일 생성/수정 입력. */
    public record TodoCommand(
            String title,
            String subject,
            LocalDate planDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
