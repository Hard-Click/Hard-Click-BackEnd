package com.wanted.backend.domain.learning_activity.application.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoCompletionOutboxRelayTest {

    private VideoCompletionOutboxStore outboxStore;
    private VideoCompletionDispatcher dispatcher;
    private VideoCompletionOutboxRelay relay;

    @BeforeEach
    void setUp() {
        outboxStore = mock(VideoCompletionOutboxStore.class);
        dispatcher = mock(VideoCompletionDispatcher.class);
        relay = new VideoCompletionOutboxRelay(outboxStore, dispatcher);
    }

    @Test
    void 성공한_메시지는_DONE_실패한_메시지는_재시도로_되돌린다() {
        OutboxMessage ok = new OutboxMessage(1L, 77L, 55L, 42L, Instant.parse("2026-01-03T00:00:00Z"));
        OutboxMessage failing = new OutboxMessage(2L, 88L, 66L, 42L, Instant.parse("2026-01-03T00:00:00Z"));
        when(outboxStore.claimBatch(anyInt())).thenReturn(List.of(ok, failing));
        doThrow(new RuntimeException("boom")).when(dispatcher).dispatch(failing);

        relay.relay();

        // 개별 실패가 배치를 멈추지 않는다 — 둘 다 dispatch 시도됨
        verify(dispatcher).dispatch(ok);
        verify(dispatcher).dispatch(failing);
        // 성공 → DONE, 실패 → markFailed(backoff 재시도)
        verify(outboxStore).markDone(1L);
        verify(outboxStore).markFailed(eq(2L), any());
        verify(outboxStore, never()).markDone(2L);
    }
}
