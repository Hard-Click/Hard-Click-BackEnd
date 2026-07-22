package com.wanted.backend.domain.study_schedule.application.usecase;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;

/**
 * 학생이 직접 추가한 할 일 CRUD.
 *
 * <p>AI 슬롯({@link ScheduleUseCase#completeSlot})과 달리 학생이 만들고 고치고 지운다.
 */
public interface StudentTodoUseCase {

    /** 할 일 추가. @return 생성된 할 일 ID */
    Long create(Long memberId, ScheduleDtos.TodoCommand command);

    /** 할 일 수정. 완료 상태는 안 바뀐다. */
    void update(Long memberId, Long todoId, ScheduleDtos.TodoCommand command);

    /** 할 일 삭제. */
    void delete(Long memberId, Long todoId);

    /** 할 일 완료 체크. */
    void complete(Long memberId, Long todoId);

    /** 할 일 완료 취소(PLANNED 로 되돌림). */
    void incomplete(Long memberId, Long todoId);
}
