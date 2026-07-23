package com.wanted.backend.domain.learning_activity.application.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * durable outbox를 폴링해 완료 이벤트를 소비자에게 재전달하는 relay.
 *
 * <p>due 행을 배치로 선점(claimBatch)해 각 메시지를 dispatch하고, 성공하면 DONE, 실패하면 backoff 재시도로
 * 되돌린다(최대 시도 초과 시 DEAD). 개별 메시지 실패가 배치 전체를 멈추지 않도록 메시지 단위로 격리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoCompletionOutboxRelay {

    private final VideoCompletionOutboxStore outboxStore;
    private final VideoCompletionDispatcher dispatcher;

    @Value("${learning.video-completion-outbox.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedRateString = "${learning.video-completion-outbox.poll-rate-ms:5000}")
    public void relay() {
        List<OutboxMessage> batch = outboxStore.claimBatch(batchSize);
        for (OutboxMessage message : batch) {
            try {
                dispatcher.dispatch(message);
                outboxStore.markDone(message.id(), message.claimedAttempt());
            } catch (Exception exception) {
                // 전달 실패 — backoff 재시도로 되돌린다. 소비자가 멱등하므로 재전달돼도 중복 집계되지 않는다.
                outboxStore.markFailed(message.id(), exception.getMessage(), message.claimedAttempt());
            }
        }
    }
}
