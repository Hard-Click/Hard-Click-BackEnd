package com.wanted.backend.domain.learning_activity.application.outbox;

import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;

/**
 * 완료 이벤트를 durable하게 소비하는 대상의 SPI. 랭킹·수강량 잔디 등 카운트형 소비자가 구현한다.
 *
 * <p>relay가 재전달할 수 있으므로 구현체는 반드시 <b>멱등</b>해야 한다(processed_video_completion 선점).
 * 처리에 실패하면 예외를 던져 relay가 재시도하게 한다. 이미 처리된 완료(선점 실패)는 성공으로 간주해 조용히 반환한다.
 */
public interface VideoCompletionConsumer {

    /** dedup·로깅 식별용 소비자 이름. */
    String consumerId();

    /** 완료 1건을 멱등하게 처리한다. 실패 시 예외를 던진다(relay 재시도 유발). */
    void process(VideoCompletedEvent event);
}
