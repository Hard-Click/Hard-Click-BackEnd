package com.wanted.backend.domain.learning_activity.application.outbox;

import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * outbox 메시지를 등록된 모든 {@link VideoCompletionConsumer}에게 전달한다.
 *
 * <p>한 소비자가 실패해도 나머지는 계속 처리하고(부분 진행), 하나라도 실패하면 예외를 던져 relay가 재시도하게 한다.
 * 소비자는 멱등하므로, 재시도 시 이미 성공한 소비자는 자신의 dedup으로 조용히 스킵된다.
 */
@Slf4j
@Component
public class VideoCompletionDispatcher {

    private final List<VideoCompletionConsumer> consumers;

    public VideoCompletionDispatcher(List<VideoCompletionConsumer> consumers) {
        this.consumers = consumers;
    }

    public void dispatch(OutboxMessage message) {
        VideoCompletedEvent event = new VideoCompletedEvent(
                message.memberId(), message.videoId(), message.courseId(), message.occurredAt());

        List<String> failed = new ArrayList<>();
        for (VideoCompletionConsumer consumer : consumers) {
            try {
                consumer.process(event);
            } catch (Exception exception) {
                failed.add(consumer.consumerId());
                log.warn("[Outbox] 소비자 처리 실패 — 재시도 대상. consumer={}, memberId={}, videoId={}",
                        consumer.consumerId(), message.memberId(), message.videoId(), exception);
            }
        }

        if (!failed.isEmpty()) {
            throw new VideoCompletionDispatchException(failed);
        }
    }
}
