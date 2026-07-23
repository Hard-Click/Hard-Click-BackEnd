package com.wanted.backend.domain.learning_activity.application.outbox;

import java.time.Instant;

/**
 * relay가 소비자에게 재전달할 완료 메시지(영속 엔티티 대신 application 경계에서 쓰는 DTO).
 */
public record OutboxMessage(
        Long id,
        Long memberId,
        Long videoId,
        Long courseId,
        Instant occurredAt
) {
}
