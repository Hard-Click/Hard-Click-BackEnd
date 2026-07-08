package com.wanted.backend.domain.quiz.infrastructure.enrollment;

import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

// enrollment 테이블의 읽기 전용 참조 (Port + ReferenceEntity 패턴) — 수강 여부 확인용.
// learning_activity의 동명 엔티티와 동일 방식으로 도메인별로 격리한다.
@Entity
@Getter
@Immutable
@Table(name = "enrollment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrollmentReferenceJpaEntity {

    @Id
    @Column(name = "enrollment_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
}
