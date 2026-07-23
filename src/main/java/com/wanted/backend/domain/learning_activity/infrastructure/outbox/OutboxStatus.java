package com.wanted.backend.domain.learning_activity.infrastructure.outbox;

/**
 * video_completion_outbox 행의 처리 상태.
 *
 * <p>PENDING(대기·재시도 예정) → PROCESSING(relay 선점) → DONE(완료) | DEAD(최대 시도 초과·운영 확인 대상).
 */
public enum OutboxStatus {
    PENDING,
    PROCESSING,
    DONE,
    DEAD
}
