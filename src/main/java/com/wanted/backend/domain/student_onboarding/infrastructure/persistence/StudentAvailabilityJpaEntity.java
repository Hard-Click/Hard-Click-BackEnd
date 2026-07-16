package com.wanted.backend.domain.student_onboarding.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * student_availability (V3.1.2 생성, V3.2.3 에서 member 단위로 전환) - 학생의 '가능한' 시간 구간.
 *
 * <p>화면은 불가능한 칸을 받지만 이 테이블은 가능 구간을 저장한다 -
 * 변환은 {@link com.wanted.backend.domain.student_onboarding.domain.model.WeeklyAvailability} 가 한다.
 */
@Entity
@Getter
@Table(name = "student_availability")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentAvailabilityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 0=일 ... 6=토 */
    @Column(name = "day_of_week", nullable = false)
    private Byte dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public static StudentAvailabilityJpaEntity create(
            Long memberId, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        StudentAvailabilityJpaEntity e = new StudentAvailabilityJpaEntity();
        e.memberId = memberId;
        e.dayOfWeek = (byte) dayOfWeek;
        e.startTime = startTime;
        e.endTime = endTime;
        return e;
    }
}
