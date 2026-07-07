package com.wanted.backend.domain.enrollment_management.application.service;

import com.wanted.backend.domain.enrollment_management.application.port.MyEnrolledCourseQueryPort;
import com.wanted.backend.domain.enrollment_management.domain.model.EnrollmentStatus;
import com.wanted.backend.domain.enrollment_management.domain.repository.EnrollmentRepository;
import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentStatusUpdater {

    private final MyEnrolledCourseQueryPort myEnrolledCourseQueryPort;
    private final EnrollmentRepository enrollmentRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(VideoCompletedEvent event) {
        boolean allLessonsCompleted = myEnrolledCourseQueryPort.findByMemberId(event.memberId())
                .stream()
                .filter(data -> data.courseId().equals(event.courseId()))
                .anyMatch(data -> data.totalLessonCount() != null
                        && data.totalLessonCount() > 0
                        && data.totalLessonCount().equals(data.completedLessonCount()));

        if (!allLessonsCompleted) {
            return;
        }

        enrollmentRepository.findByMemberIdAndCourseId(event.memberId(), event.courseId())
                .filter(e -> !EnrollmentStatus.COMPLETED.equals(e.getEffectiveStatus()))
                .ifPresent(enrollment -> {
                    try {
                        enrollmentRepository.save(enrollment.complete());
                    } catch (Exception ex) {
                        log.error("[Enrollment] status COMPLETED 전환 실패. memberId={}, courseId={}",
                                event.memberId(), event.courseId(), ex);
                    }
                });
    }
}
