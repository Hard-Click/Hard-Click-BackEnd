package com.wanted.backend.domain.enrollment_management.infrastructure.persistence;

import com.wanted.backend.domain.enrollment_management.domain.model.Enrollment;
import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "enrollment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enrollment_member_course",
                columnNames = {"member_id", "course_id"}
        ),
        indexes = @Index(name = "idx_enrollment_course_id", columnList = "course_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrollmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.IN_PROGRESS;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    static EnrollmentJpaEntity from(Enrollment enrollment) {
        EnrollmentJpaEntity entity = new EnrollmentJpaEntity();
        entity.id = enrollment.getId();
        entity.memberId = enrollment.getMemberId();
        entity.courseId = enrollment.getCourseId();
        entity.enrolledAt = enrollment.getEnrolledAt();
        entity.status = enrollment.getStatus();
        entity.expiredAt = enrollment.getExpiredAt();
        return entity;
    }

    Enrollment toDomain() {
        return Enrollment.restore(id, memberId, courseId, enrolledAt, status, expiredAt);
    }
}
