package com.wanted.backend.domain.enrollment_management.domain.repository;

import com.wanted.backend.domain.enrollment_management.domain.model.Enrollment;
import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;

import java.util.List;

public interface EnrollmentRepository {
    boolean existsByMemberIdAndCourseId(Long memberId, Long courseId);
    Enrollment save(Enrollment enrollment);
    List<Enrollment> findByMemberId(Long memberId);
    List<Enrollment> findByMemberIdAndStatus(Long memberId, EnrollmentStatus status);
    java.util.Optional<Enrollment> findByMemberIdAndCourseId(Long memberId, Long courseId);
}
