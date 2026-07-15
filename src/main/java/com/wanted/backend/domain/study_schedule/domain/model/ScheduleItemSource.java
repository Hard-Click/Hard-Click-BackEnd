package com.wanted.backend.domain.study_schedule.domain.model;

/**
 * 스케줄 화면 항목의 출처. 캘린더/오늘 할 일은 두 소스를 합쳐서 보여준다.
 *
 * <p>프론트는 이 값으로 완료 처리 엔드포인트를 고른다 - 소유 테이블이 다르기 때문이다.
 */
public enum ScheduleItemSource {

    /** AI가 배치한 강의 슬롯(schedule_slot). 완료: PATCH /api/schedule/slots/{id}/complete */
    LESSON,

    /** 학생이 직접 추가한 할 일(student_todo). 완료: PATCH /api/schedule/todos/{id}/complete */
    TODO
}
