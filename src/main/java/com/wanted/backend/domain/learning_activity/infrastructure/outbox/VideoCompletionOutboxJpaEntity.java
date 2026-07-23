package com.wanted.backend.domain.learning_activity.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "video_completion_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoCompletionOutboxJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // 이벤트 발생 instant를 UTC 기준 datetime(6)으로 보관한다(복원 시 UTC로 되돌린다).
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public VideoCompletionOutboxJpaEntity(
            Long memberId,
            Long videoId,
            Long courseId,
            LocalDateTime occurredAt,
            LocalDateTime nextAttemptAt
    ) {
        this.memberId = memberId;
        this.videoId = videoId;
        this.courseId = courseId;
        this.occurredAt = occurredAt;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = nextAttemptAt;
    }

    /** relay 선점: PROCESSING으로 바꾸고 시도 횟수를 올리며, 가시성 타임아웃까지 다음 시도를 미룬다. */
    public void markProcessing(LocalDateTime visibilityDeadline) {
        this.status = OutboxStatus.PROCESSING;
        this.attempts += 1;
        this.nextAttemptAt = visibilityDeadline;
    }

    public void markDone(LocalDateTime now) {
        this.status = OutboxStatus.DONE;
        this.processedAt = now;
        this.lastError = null;
    }

    /** 재시도 예약(backoff) 또는 최대 시도 초과 시 DEAD 전환. */
    public void reschedule(LocalDateTime nextAttemptAt, String error, boolean dead) {
        this.status = dead ? OutboxStatus.DEAD : OutboxStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = error;
    }
}
