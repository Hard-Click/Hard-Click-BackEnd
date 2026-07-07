package com.wanted.backend.domain.enrollment_management.infrastructure.persistence;

import com.wanted.backend.domain.enrollment_management.domain.model.Enrollment;
import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import com.wanted.backend.domain.enrollment_management.domain.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EnrollmentRepositoryAdapter implements EnrollmentRepository {

    private final SpringDataEnrollmentRepository jpaRepository;

    @Override
    public boolean existsByMemberIdAndCourseId(Long memberId, Long courseId) {
        return jpaRepository.existsByMemberIdAndCourseId(memberId, courseId);
    }

    @Override
    public Enrollment save(Enrollment enrollment) {
        return jpaRepository.save(EnrollmentJpaEntity.from(enrollment)).toDomain();
    }

    @Override
    public List<Enrollment> findByMemberId(Long memberId) {
        return jpaRepository.findByMemberId(memberId).stream()
                .map(EnrollmentJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Enrollment> findByMemberIdAndStatus(Long memberId, EnrollmentStatus status) {
        return jpaRepository.findByMemberIdAndStatus(memberId, status).stream()
                .map(EnrollmentJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Enrollment> findByMemberIdAndCourseId(Long memberId, Long courseId) {
        return jpaRepository.findByMemberIdAndCourseId(memberId, courseId)
                .map(EnrollmentJpaEntity::toDomain);
    }
}
