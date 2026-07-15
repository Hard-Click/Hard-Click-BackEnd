package com.wanted.backend.domain.study_schedule.application.port;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;

import java.time.LocalDate;
import java.util.List;

/**
 * 학생이 직접 추가한 할 일(student_todo) 아웃바운드 포트.
 *
 * <p>AI 슬롯({@link SchedulePlanPort})과 소유가 다르므로 포트를 분리한다 -
 * 저쪽은 AI가 쓰고 학생은 완료 체크만, 이쪽은 학생이 CRUD 한다.
 */
public interface StudentTodoPort {

    /** 기간 내 회원의 할 일 목록. */
    List<ScheduleDtos.CalendarItem> findTodos(Long memberId, LocalDate from, LocalDate to);

    /** 할 일 추가. @return 생성된 id */
    Long create(Long memberId, ScheduleDtos.TodoCommand command);

    /** 할 일 수정. @return 갱신 여부(false = 없음/타인 소유) */
    boolean update(Long memberId, Long todoId, ScheduleDtos.TodoCommand command);

    /** 할 일 삭제. @return 삭제 여부(false = 없음/타인 소유) */
    boolean delete(Long memberId, Long todoId);

    /** 할 일 완료 체크. @return 갱신 여부(false = 없음/타인 소유) */
    boolean markDone(Long memberId, Long todoId);
}
