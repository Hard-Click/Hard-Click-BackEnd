package com.wanted.backend.domain.enrollment_management.application.listener;

import com.wanted.backend.domain.enrollment_management.application.port.ScheduleGenerationPort;
import com.wanted.backend.domain.enrollment_management.domain.event.EnrollmentStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 수강 시작 이벤트를 받아 AI 스케줄러(Python)에 주간 스케줄 즉시 생성을 요청한다.
 * 커밋 이후(AFTER_COMMIT)에 실행해야 Python이 방금 생긴 enrollment 행을 읽을 수 있고,
 * 생성 실패가 수강신청 저장을 되돌리지 않는다(미생성분은 주간 배치가 보정).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleGenerationListener {

    private final ScheduleGenerationPort scheduleGenerationPort;

    @Async("schedulerAiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnrollmentStarted(EnrollmentStartedEvent event) {
        try {
            scheduleGenerationPort.requestGeneration(event.memberId());
        } catch (Exception e) {
            // 스케줄 생성 실패는 수강신청 결과에 영향을 주지 않는다 — 로깅만 하고 삼킨다.
            log.error("스케줄 즉시 생성 요청 실패 (memberId={}, courseId={}): {}",
                    event.memberId(), event.courseId(), e.getMessage(), e);
        }
    }
}
