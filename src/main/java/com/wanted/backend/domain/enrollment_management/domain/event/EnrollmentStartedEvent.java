package com.wanted.backend.domain.enrollment_management.domain.event;

/**
 * 수강이 시작(신규 등록 또는 재수강 재활성화)되어 IN_PROGRESS가 된 시점의 이벤트.
 * AFTER_COMMIT 리스너가 AI 스케줄러 즉시 생성 등 후처리에 사용한다.
 */
public record EnrollmentStartedEvent(Long memberId, Long enrollmentId, Long courseId) {
}
