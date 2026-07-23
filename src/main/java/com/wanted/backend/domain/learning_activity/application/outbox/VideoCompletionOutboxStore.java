package com.wanted.backend.domain.learning_activity.application.outbox;

import java.time.Instant;
import java.util.List;

/**
 * VideoCompletedEvent durable outbox 저장소 포트.
 *
 * <p>{@link #enqueue}는 완료 트랜잭션과 같은 커밋으로 호출돼야 한다(크래시 durability의 전제). 나머지는 relay가
 * 폴링·전달·상태전이에 쓴다.
 */
public interface VideoCompletionOutboxStore {

    /** 완료 이벤트를 outbox에 적재한다(호출자의 트랜잭션에 참여). */
    void enqueue(Long memberId, Long videoId, Long courseId, Instant occurredAt);

    /** 처리 가능한 행을 최대 {@code limit}개 선점(PROCESSING)해 가져온다. */
    List<OutboxMessage> claimBatch(int limit);

    /**
     * 전달 성공 — 해당 행을 DONE으로 종료한다.
     * {@code claimedAttempt}가 현재 lease 세대와 일치하고 아직 PROCESSING일 때만 반영한다(그 외엔 무시).
     */
    void markDone(Long id, int claimedAttempt);

    /**
     * 전달 실패 — backoff 후 재시도 예약하거나, 최대 시도를 넘으면 DEAD로 종료한다.
     * {@code claimedAttempt}가 현재 lease 세대와 일치하고 아직 PROCESSING일 때만 반영한다(그 외엔 무시).
     */
    void markFailed(Long id, String error, int claimedAttempt);
}
