package com.wanted.backend.domain.study_schedule.application.service;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.StudentTodoPort;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentTodoServiceTest {

    private static final LocalDate PLAN_DATE = LocalDate.of(2026, 7, 15);
    private static final Long MEMBER_ID = 77001L;

    private StudentTodoPort studentTodoPort;
    private StudentTodoService studentTodoService;

    @BeforeEach
    void setUp() {
        studentTodoPort = mock(StudentTodoPort.class);
        studentTodoService = new StudentTodoService(studentTodoPort);
    }

    private static ScheduleDtos.TodoCommand command(LocalTime start, LocalTime end) {
        return new ScheduleDtos.TodoCommand("지난주 복습 퀴즈", "복습", PLAN_DATE, start, end);
    }

    @Test
    void createsTodoWithTimeRange() {
        when(studentTodoPort.create(anyLong(), any())).thenReturn(5L);

        Long id = studentTodoService.create(MEMBER_ID, command(LocalTime.of(20, 0), LocalTime.of(21, 0)));

        assertThat(id).isEqualTo(5L);
    }

    /** 시간을 아예 안 적는 할 일은 허용 — 타임테이블엔 안 뜨고 목록에만 보인다. */
    @Test
    void allowsTodoWithoutAnyTime() {
        when(studentTodoPort.create(anyLong(), any())).thenReturn(6L);

        assertThatCode(() -> studentTodoService.create(MEMBER_ID, command(null, null)))
                .doesNotThrowAnyException();
    }

    /** 시작만 있으면 블록 길이를 알 수 없어 타임테이블에 그릴 수 없다. */
    @Test
    void rejectsTodoWithOnlyOneSideOfTimeRange() {
        assertThatThrownBy(() -> studentTodoService.create(MEMBER_ID, command(LocalTime.of(20, 0), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TODO_TIME_RANGE_INCOMPLETE);

        assertThatThrownBy(() -> studentTodoService.create(MEMBER_ID, command(null, LocalTime.of(21, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TODO_TIME_RANGE_INCOMPLETE);

        verify(studentTodoPort, never()).create(anyLong(), any());
    }

    @Test
    void rejectsInvertedOrZeroLengthTimeRange() {
        assertThatThrownBy(() -> studentTodoService.create(MEMBER_ID, command(LocalTime.of(21, 0), LocalTime.of(20, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TODO_TIME_RANGE_INVALID);

        assertThatThrownBy(() -> studentTodoService.create(MEMBER_ID, command(LocalTime.of(20, 0), LocalTime.of(20, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TODO_TIME_RANGE_INVALID);
    }

    /** 남의 할 일을 id 만 알고 건드리면 404 — 어댑터가 소유 확인 후 false 를 준다. */
    @Test
    void rejectsOperationsOnTodoNotOwnedByMember() {
        when(studentTodoPort.update(anyLong(), anyLong(), any())).thenReturn(false);
        when(studentTodoPort.delete(anyLong(), anyLong())).thenReturn(false);
        when(studentTodoPort.markDone(anyLong(), anyLong())).thenReturn(false);
        when(studentTodoPort.markPlanned(anyLong(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> studentTodoService.update(MEMBER_ID, 999L, command(null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TODO_NOT_FOUND);

        assertThatThrownBy(() -> studentTodoService.delete(MEMBER_ID, 999L))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> studentTodoService.complete(MEMBER_ID, 999L))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> studentTodoService.incomplete(MEMBER_ID, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TODO_NOT_FOUND);
    }

    @Test
    void completesOwnedTodo() {
        when(studentTodoPort.markDone(MEMBER_ID, 5L)).thenReturn(true);

        assertThatCode(() -> studentTodoService.complete(MEMBER_ID, 5L)).doesNotThrowAnyException();
        verify(studentTodoPort).markDone(MEMBER_ID, 5L);
    }

    @Test
    void incompletesOwnedTodo() {
        when(studentTodoPort.markPlanned(MEMBER_ID, 5L)).thenReturn(true);

        assertThatCode(() -> studentTodoService.incomplete(MEMBER_ID, 5L)).doesNotThrowAnyException();
        verify(studentTodoPort).markPlanned(MEMBER_ID, 5L);
    }
}
