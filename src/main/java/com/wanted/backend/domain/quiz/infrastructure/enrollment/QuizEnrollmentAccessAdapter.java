package com.wanted.backend.domain.quiz.infrastructure.enrollment;

import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import com.wanted.backend.domain.quiz.application.port.EnrollmentAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizEnrollmentAccessAdapter implements EnrollmentAccessPort {

    // learning_activity의 수강 접근 정책과 동일하게 IN_PROGRESS/COMPLETED만 유효 수강으로 본다.
    private static final Set<EnrollmentStatus> ACTIVE_ENROLLMENT_STATUSES =
            EnumSet.of(EnrollmentStatus.IN_PROGRESS, EnrollmentStatus.COMPLETED);

    private final QuizEnrollmentJpaRepository repository;

    @Override
    public boolean hasActiveEnrollment(Long memberId, Long courseId) {
        return repository.existsByMemberIdAndCourseIdAndStatusInAndExpiredAtIsNull(
                memberId, courseId, ACTIVE_ENROLLMENT_STATUSES)
                || repository.existsByMemberIdAndCourseIdAndStatusInAndExpiredAtGreaterThanEqual(
                memberId, courseId, ACTIVE_ENROLLMENT_STATUSES, LocalDateTime.now());
    }
}
