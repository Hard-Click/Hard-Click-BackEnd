package com.wanted.backend.domain.learning_activity.infrastructure.outbox;

import com.wanted.backend.domain.learning_activity.application.outbox.OutboxMessage;
import com.wanted.backend.domain.learning_activity.application.outbox.VideoCompletionOutboxStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class VideoCompletionOutboxStoreAdapter implements VideoCompletionOutboxStore {

    private static final int MAX_ERROR_LENGTH = 500;

    private final SpringDataVideoCompletionOutboxRepository repository;
    private final Clock clock;

    @Value("${learning.video-completion-outbox.max-attempts:10}")
    private int maxAttempts;
    @Value("${learning.video-completion-outbox.base-backoff-seconds:10}")
    private long baseBackoffSeconds;
    @Value("${learning.video-completion-outbox.max-backoff-seconds:3600}")
    private long maxBackoffSeconds;
    @Value("${learning.video-completion-outbox.visibility-timeout-seconds:120}")
    private long visibilityTimeoutSeconds;

    @Override
    @Transactional
    public void enqueue(Long memberId, Long videoId, Long courseId, Instant occurredAt) {
        LocalDateTime now = LocalDateTime.now(clock);
        // instant를 UTC datetime(6)으로 보관 → 소비자가 UTC로 복원해 원래 instant를 되찾는다.
        LocalDateTime occurredAtUtc = LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC);
        repository.save(new VideoCompletionOutboxJpaEntity(memberId, videoId, courseId, occurredAtUtc, now));
    }

    @Override
    @Transactional
    public List<OutboxMessage> claimBatch(int limit) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime visibilityDeadline = now.plusSeconds(visibilityTimeoutSeconds);

        List<VideoCompletionOutboxJpaEntity> rows =
                repository.findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAt(
                        List.of(OutboxStatus.PENDING, OutboxStatus.PROCESSING),
                        now,
                        PageRequest.of(0, limit));
        List<OutboxMessage> messages = new ArrayList<>(rows.size());
        for (VideoCompletionOutboxJpaEntity row : rows) {
            row.markProcessing(visibilityDeadline);   // 변경분은 트랜잭션 커밋 시 dirty checking으로 반영
            messages.add(new OutboxMessage(
                    row.getId(),
                    row.getMemberId(),
                    row.getVideoId(),
                    row.getCourseId(),
                    row.getOccurredAt().toInstant(ZoneOffset.UTC),
                    row.getAttempts()));   // 선점 시점의 lease 세대 — 종료 전이 시 소유권 검증에 쓴다
        }
        return messages;
    }

    @Override
    @Transactional
    public void markDone(Long id, int claimedAttempt) {
        // 잠금 조회로 최신 커밋값을 읽어, 재선점 여부를 놓치지 않는다.
        repository.findWithLockById(id)
                .filter(row -> row.isHeldBy(claimedAttempt))   // 아직 이 relay 세대가 소유 중일 때만
                .ifPresent(row -> row.markDone(LocalDateTime.now(clock)));
    }

    @Override
    @Transactional
    public void markFailed(Long id, String error, int claimedAttempt) {
        repository.findWithLockById(id)
                .filter(row -> row.isHeldBy(claimedAttempt))   // 소유권을 잃은(재선점된) 세대는 상태를 되돌리지 않는다
                .ifPresent(row -> {
                    LocalDateTime now = LocalDateTime.now(clock);
                    boolean dead = row.getAttempts() >= maxAttempts;
                    LocalDateTime nextAttemptAt = dead ? now : now.plusSeconds(backoffSeconds(row.getAttempts()));
                    row.reschedule(nextAttemptAt, truncate(error), dead);
                });
    }

    // 지수 backoff: base * 2^(attempts-1), 상한 cap. attempts는 claim 시 이미 증가돼 최소 1이다.
    private long backoffSeconds(int attempts) {
        int exponent = Math.min(Math.max(0, attempts - 1), 20);
        double raw = baseBackoffSeconds * Math.pow(2, exponent);
        return (long) Math.min(raw, maxBackoffSeconds);
    }

    private String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }
}
