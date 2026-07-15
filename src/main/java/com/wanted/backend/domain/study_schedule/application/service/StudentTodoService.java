package com.wanted.backend.domain.study_schedule.application.service;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.StudentTodoPort;
import com.wanted.backend.domain.study_schedule.application.usecase.StudentTodoUseCase;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentTodoService implements StudentTodoUseCase {

    private final StudentTodoPort studentTodoPort;

    @Override
    public Long create(Long memberId, ScheduleDtos.TodoCommand command) {
        validateTimeRange(command);
        return studentTodoPort.create(memberId, command);
    }

    @Override
    public void update(Long memberId, Long todoId, ScheduleDtos.TodoCommand command) {
        validateTimeRange(command);
        if (!studentTodoPort.update(memberId, todoId, command)) {
            throw new BusinessException(ErrorCode.TODO_NOT_FOUND);
        }
    }

    @Override
    public void delete(Long memberId, Long todoId) {
        if (!studentTodoPort.delete(memberId, todoId)) {
            throw new BusinessException(ErrorCode.TODO_NOT_FOUND);
        }
    }

    @Override
    public void complete(Long memberId, Long todoId) {
        if (!studentTodoPort.markDone(memberId, todoId)) {
            throw new BusinessException(ErrorCode.TODO_NOT_FOUND);
        }
    }

    /**
     * 시간 규칙 - 타임테이블에 그릴 수 있는 형태인지 본다.
     *
     * <p>시간을 아예 안 적는 건 허용한다(타임테이블엔 안 뜨고 목록에만 보이는 할 일).
     * 다만 시작만 적고 종료를 안 적으면 블록 길이를 알 수 없고, 종료가 시작보다 앞서면 블록이 뒤집힌다.
     * Bean Validation 으로는 필드 간 관계를 못 보므로 여기서 막는다.
     */
    private void validateTimeRange(ScheduleDtos.TodoCommand command) {
        boolean hasStart = command.startTime() != null;
        boolean hasEnd = command.endTime() != null;

        if (hasStart != hasEnd) {
            throw new BusinessException(ErrorCode.TODO_TIME_RANGE_INCOMPLETE);
        }
        if (hasStart && !command.endTime().isAfter(command.startTime())) {
            throw new BusinessException(ErrorCode.TODO_TIME_RANGE_INVALID);
        }
    }
}
