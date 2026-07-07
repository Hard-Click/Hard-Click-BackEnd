package com.wanted.backend.domain.enrollment_management.infrastructure.persistence;

import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataEnrollmentRepository extends JpaRepository<EnrollmentJpaEntity, Long> {
    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);
    List<EnrollmentJpaEntity> findByMemberId(Long memberId);
    List<EnrollmentJpaEntity> findByMemberIdAndStatus(Long memberId, EnrollmentStatus status);
    java.util.Optional<EnrollmentJpaEntity> findByMemberIdAndCourseId(Long memberId, Long courseId);
}
