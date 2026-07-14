package com.wanted.backend.domain.enrollment_management.application.service;

import com.wanted.backend.domain.enrollment_management.application.command.EnrollCommand;
import com.wanted.backend.domain.enrollment_management.application.usecase.EnrollUseCase;
import com.wanted.backend.domain.enrollment_management.domain.model.Enrollment;
import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import com.wanted.backend.domain.enrollment_management.domain.repository.EnrollmentRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollService implements EnrollUseCase {

    private final EnrollmentRepository enrollmentRepository;
    private final Clock clock;

    @Override
    public Long handle(EnrollCommand command) {
        Optional<Enrollment> existing =
                enrollmentRepository.findByMemberIdAndCourseId(command.memberId(), command.courseId());

        if (existing.isPresent()) {
            Enrollment enrollment = existing.get();
            // ACTIVE(IN_PROGRESS/COMPLETED, 미만료)만 "이미 수강중"으로 본다.
            // (learning_activity/quiz EnrollmentAccessAdapter의 유효 수강 정책과 동일 기준)
            if (isActive(enrollment)) {
                throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
            }
            // REFUNDED/EXPIRED 등 비활성 행은 재수강 대상 → 기존 행을 재활성화한다.
            // enrollment 테이블에 uk_enrollment_member_course(member_id, course_id) 유니크 제약이
            // 있어 새 행 INSERT는 제약 위반으로 실패하므로, 반드시 기존 행 UPDATE로 처리한다.
            enrollment.reactivate(Instant.now(clock));
            return enrollmentRepository.save(enrollment).getId();
        }

        Enrollment enrollment = Enrollment.create(command.memberId(), command.courseId(), Instant.now(clock));
        try {
            // IDENTITY 전략이라 save 시 INSERT가 즉시 flush됨 → 동시 최초 등록 레이스는 여기서 표면화된다.
            return enrollmentRepository.save(enrollment).getId();
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 다른 스레드가 먼저 INSERT하여 uk_enrollment_member_course 위반 →
            // 이미 수강 중인 것과 동일하므로 500 대신 멱등하게 ENROLLMENT_ALREADY_EXISTS로 매핑한다.
            throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
        }
    }

    private boolean isActive(Enrollment enrollment) {
        EnrollmentStatus status = enrollment.getEffectiveStatus();
        return status == EnrollmentStatus.IN_PROGRESS || status == EnrollmentStatus.COMPLETED;
    }
}
