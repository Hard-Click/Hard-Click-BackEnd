package com.wanted.backend.domain.study_schedule.infrastructure.persistence;

import com.wanted.backend.domain.study_schedule.domain.model.TodoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * student_todo (V3.2.5) - 학생이 직접 추가한 할 일.
 *
 * <p>schedule_slot 과 달리 weekly_schedule 스냅샷에 묶이지 않는다 - 리플로우가 날리면 안 되기 때문.
 */
@Entity
@Getter
@Table(name = "student_todo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentTodoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "subject", length = 30)
    private String subject;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TodoStatus status;

    public static StudentTodoJpaEntity create(
            Long memberId, String title, String subject,
            LocalDate planDate, LocalTime startTime, LocalTime endTime) {
        StudentTodoJpaEntity e = new StudentTodoJpaEntity();
        e.memberId = memberId;
        e.title = title;
        e.subject = subject;
        e.planDate = planDate;
        e.startTime = startTime;
        e.endTime = endTime;
        e.status = TodoStatus.PLANNED;
        return e;
    }

    /** 수정은 완료 상태를 건드리지 않는다 - 완료 체크는 별도 엔드포인트다. */
    public void update(String title, String subject, LocalDate planDate, LocalTime startTime, LocalTime endTime) {
        this.title = title;
        this.subject = subject;
        this.planDate = planDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void markDone() {
        this.status = TodoStatus.DONE;
    }

    /** 완료 취소 - 실수 체크·마음 변경 시 되돌린다. LESSON 슬롯과 달리 TODO는 양방향이다. */
    public void markPlanned() {
        this.status = TodoStatus.PLANNED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
