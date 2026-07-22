package com.wanted.backend.domain.study_schedule.infrastructure.persistence;

import com.wanted.backend.domain.study_schedule.application.dto.ScheduleDtos;
import com.wanted.backend.domain.study_schedule.application.port.StudentTodoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * student_todo JPA 어댑터.
 *
 * <p>SchedulePlanAdapter 가 native query 인 것과 달리 여기는 JPA 를 쓴다 -
 * 저쪽은 4개 테이블 조인 + 최신 스냅샷 판별이 필요하지만 이쪽은 단일 테이블 CRUD 라서다.
 */
@Component
@RequiredArgsConstructor
public class StudentTodoAdapter implements StudentTodoPort {

    private final StudentTodoJpaRepository repository;

    @Override
    public List<ScheduleDtos.CalendarItem> findTodos(Long memberId, LocalDate from, LocalDate to) {
        return repository.findByMemberIdAndPlanDateBetween(memberId, from, to).stream()
                .map(t -> ScheduleDtos.CalendarItem.ofTodo(
                        t.getId(), t.getPlanDate(), t.getStartTime(), t.getEndTime(),
                        t.getSubject(), t.getTitle(), t.getStatus().name()))
                .toList();
    }

    @Override
    public Long create(Long memberId, ScheduleDtos.TodoCommand command) {
        StudentTodoJpaEntity saved = repository.save(StudentTodoJpaEntity.create(
                memberId, command.title(), command.subject(),
                command.planDate(), command.startTime(), command.endTime()));
        return saved.getId();
    }

    @Override
    public boolean update(Long memberId, Long todoId, ScheduleDtos.TodoCommand command) {
        return findOwned(memberId, todoId)
                .map(todo -> {
                    todo.update(command.title(), command.subject(),
                            command.planDate(), command.startTime(), command.endTime());
                    return true;
                })
                .orElse(false);
    }

    @Override
    public boolean delete(Long memberId, Long todoId) {
        return findOwned(memberId, todoId)
                .map(todo -> {
                    repository.delete(todo);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public boolean markDone(Long memberId, Long todoId) {
        return findOwned(memberId, todoId)
                .map(todo -> {
                    todo.markDone();
                    return true;
                })
                .orElse(false);
    }

    @Override
    public boolean markPlanned(Long memberId, Long todoId) {
        return findOwned(memberId, todoId)
                .map(todo -> {
                    todo.markPlanned();
                    return true;
                })
                .orElse(false);
    }

    /** 남의 할 일을 id 만 알고 건드리지 못하게 소유를 함께 확인한다. */
    private java.util.Optional<StudentTodoJpaEntity> findOwned(Long memberId, Long todoId) {
        return repository.findById(todoId).filter(todo -> todo.isOwnedBy(memberId));
    }
}
