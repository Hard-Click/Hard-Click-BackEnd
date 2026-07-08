package com.wanted.backend.domain.quiz.infrastructure.enrollment;

import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;

public interface QuizEnrollmentJpaRepository extends JpaRepository<QuizEnrollmentReferenceJpaEntity, Long> {

    boolean existsByMemberIdAndCourseIdAndStatusInAndExpiredAtIsNull(
            Long memberId, Long courseId, Collection<EnrollmentStatus> statuses);

    boolean existsByMemberIdAndCourseIdAndStatusInAndExpiredAtGreaterThanEqual(
            Long memberId, Long courseId, Collection<EnrollmentStatus> statuses, LocalDateTime now);

    // 통계용 활성 수강생 목록 — 무기한(만료 null) + 미만료(만료일 미래) 두 갈래를 각각 조회한다.
    java.util.List<QuizEnrollmentReferenceJpaEntity> findByCourseIdAndStatusInAndExpiredAtIsNull(
            Long courseId, Collection<EnrollmentStatus> statuses);

    java.util.List<QuizEnrollmentReferenceJpaEntity> findByCourseIdAndStatusInAndExpiredAtGreaterThanEqual(
            Long courseId, Collection<EnrollmentStatus> statuses, LocalDateTime now);
}
