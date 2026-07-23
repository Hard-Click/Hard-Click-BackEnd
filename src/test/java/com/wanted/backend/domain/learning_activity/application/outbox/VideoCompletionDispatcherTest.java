package com.wanted.backend.domain.learning_activity.application.outbox;

import com.wanted.backend.domain.learning_activity.domain.event.VideoCompletedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoCompletionDispatcherTest {

    private final OutboxMessage message =
            new OutboxMessage(9L, 77L, 55L, 42L, Instant.parse("2026-01-03T00:00:00Z"));

    @Test
    void 모든_소비자에게_이벤트를_복원해_전달한다() {
        VideoCompletionConsumer consumerA = mock(VideoCompletionConsumer.class);
        VideoCompletionConsumer consumerB = mock(VideoCompletionConsumer.class);
        VideoCompletionDispatcher dispatcher = new VideoCompletionDispatcher(List.of(consumerA, consumerB));

        dispatcher.dispatch(message);

        ArgumentCaptor<VideoCompletedEvent> captor = ArgumentCaptor.forClass(VideoCompletedEvent.class);
        verify(consumerA).process(captor.capture());
        verify(consumerB).process(captor.capture());
        VideoCompletedEvent event = captor.getValue();
        assertThat(event.memberId()).isEqualTo(77L);
        assertThat(event.videoId()).isEqualTo(55L);
        assertThat(event.courseId()).isEqualTo(42L);
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-01-03T00:00:00Z"));
    }

    @Test
    void 한_소비자가_실패해도_나머지는_처리하고_예외로_재시도를_알린다() {
        VideoCompletionConsumer failing = mock(VideoCompletionConsumer.class);
        VideoCompletionConsumer healthy = mock(VideoCompletionConsumer.class);
        when(failing.consumerId()).thenReturn("ranking_lesson");
        doThrow(new RuntimeException("redis down")).when(failing).process(org.mockito.ArgumentMatchers.any());
        VideoCompletionDispatcher dispatcher = new VideoCompletionDispatcher(List.of(failing, healthy));

        assertThatThrownBy(() -> dispatcher.dispatch(message))
                .isInstanceOf(VideoCompletionDispatchException.class);

        // 실패한 소비자와 무관하게 나머지는 계속 처리된다(부분 진행 → 재시도 시 성공분은 dedup 스킵).
        verify(healthy).process(org.mockito.ArgumentMatchers.any());
    }
}
