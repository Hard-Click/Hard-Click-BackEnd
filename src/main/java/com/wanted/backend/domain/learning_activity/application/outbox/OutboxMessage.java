package com.wanted.backend.domain.learning_activity.application.outbox;

import java.time.Instant;

/**
 * relay가 소비자에게 재전달할 완료 메시지(영속 엔티티 대신 application 경계에서 쓰는 DTO).
 *
 * <p>{@code claimedAttempt}는 이 메시지를 선점한 시점의 lease 세대(claim generation)다 — 종료 전이
 * (markDone/markFailed) 시 이 값으로 소유권을 검증해, 가시성 타임아웃 후 다른 relay가 재선점한 행을
 * 뒤늦게 깨어난 이전 relay가 덮어쓰지 못하게 한다.
 */
public record OutboxMessage(
        Long id,
        Long memberId,
        Long videoId,
        Long courseId,
        Instant occurredAt,
        int claimedAttempt
) {
}
