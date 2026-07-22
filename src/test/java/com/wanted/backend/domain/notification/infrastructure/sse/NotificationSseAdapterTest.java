package com.wanted.backend.domain.notification.infrastructure.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationSseAdapterTest {

    private final NotificationSseAdapter adapter = new NotificationSseAdapter();

    @Test
    @DisplayName("sendHeartbeat: 등록된 모든 emitter에 heartbeat 이벤트를 전송한다 (ALB idle timeout 회피)")
    void sendHeartbeat_sendsToAllEmitters() throws IOException {
        SseEmitter emitter1 = mock(SseEmitter.class);
        SseEmitter emitter2 = mock(SseEmitter.class);
        register(1L, emitter1);
        register(2L, emitter2);

        adapter.sendHeartbeat();

        verify(emitter1).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter2).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("sendHeartbeat: 전송에 실패한(연결 끊긴) emitter는 레지스트리에서 제거된다")
    void sendHeartbeat_removesBrokenEmitter() throws IOException {
        SseEmitter broken = mock(SseEmitter.class);
        doThrow(new IOException("other side closed"))
                .when(broken).send(any(SseEmitter.SseEventBuilder.class));
        register(1L, broken);

        adapter.sendHeartbeat();

        assertThat(emitters()).doesNotContainKey(1L);
    }

    private void register(Long memberId, SseEmitter emitter) {
        emitters().computeIfAbsent(memberId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, List<SseEmitter>> emitters() {
        return (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(adapter, "emitters");
    }
}
