package com.wanted.backend.global.idempotency;

/**
 * VideoCompletedEvent 소비자의 멱등 처리를 위한 dedup 포트.
 *
 * <p>랭킹(Redis)·수강량 잔디(DB) 등 서로 다른 저장소를 갱신하는 소비자들이 공유한다. 각 소비자는 집계 증가
 * 전에 {@link #claim(String, Long, Long)}으로 (자신, 회원, 영상) 조합을 원자적으로 선점하고, 이미 처리된
 * 조합이면 증가를 건너뛴다. 이를 통해 동시 최초완료 경합·이벤트 재전달로 인한 '이벤트당 +1' 중복 집계를 막는다.
 */
public interface VideoCompletionDedup {

    /**
     * (consumerId, memberId, videoId) 완료 처리를 원자적으로 선점한다.
     *
     * @return 이번에 처음 선점했으면 {@code true}(→ 집계 증가 진행), 이미 처리된 조합이면 {@code false}(→ 스킵)
     */
    boolean claim(String consumerId, Long memberId, Long videoId);
}
