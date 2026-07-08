package com.wanted.backend.domain.quiz.infrastructure.enrollment;

import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;

public interface QuizEnrollmentJpaRepository extends JpaRepository<EnrollmentReferenceJpaEntity, Long> {

    boolean existsByMemberIdAndCourseIdAndStatusInAndExpiredAtIsNull(
            Long memberId, Long courseId, Collection<EnrollmentStatus> statuses);

    boolean existsByMemberIdAndCourseIdAndStatusInAndExpiredAtGreaterThanEqual(
            Long memberId, Long courseId, Collection<EnrollmentStatus> statuses, LocalDateTime now);
}
