package com.wanted.backend.domain.student_onboarding.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * student_capacity (V3.1.7 생성, V3.2.4 에서 rest_days/onboarded_at 추가) - 학생 단위 학습 용량.
 *
 * <p>daily_cap_min 은 화면 입력이 아니라 가용시간에서 유도한다
 * ({@link com.wanted.backend.domain.student_onboarding.domain.policy.DailyCapPolicy} - 임시 규칙).
 * onboarded_at 이 NULL 이면 온보딩 미완료다.
 */
@Entity
@Getter
@Table(name = "student_capacity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentCapacityJpaEntity {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "daily_cap_min")
    private Integer dailyCapMin;

    /** 휴식 요일 비트마스크 (bit0=일 ... bit6=토) */
    @Column(name = "rest_days", nullable = false)
    private Integer restDays;

    @Column(name = "onboarded_at")
    private LocalDateTime onboardedAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public static StudentCapacityJpaEntity create(Long studentId) {
        StudentCapacityJpaEntity e = new StudentCapacityJpaEntity();
        e.studentId = studentId;
        e.restDays = 0;
        return e;
    }

    public void applyAvailability(int dailyCapMin, int restDays) {
        this.dailyCapMin = dailyCapMin;
        this.restDays = restDays;
    }

    public void markOnboarded(LocalDateTime at) {
        this.onboardedAt = at;
    }

    public boolean isOnboarded() {
        return onboardedAt != null;
    }
}
