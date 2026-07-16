package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.ChatBroadcastPort;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.SystemClosedMessage;
import com.wanted.backend.domain.study.application.event.StudyClosedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudyClosedBroadcastListenerTest {

    @InjectMocks
    private StudyClosedBroadcastListener listener;

    @Mock
    private ChatBroadcastPort chatBroadcastPort;

    @Test
    @DisplayName("해산 이벤트를 받으면 SYSTEM_CLOSED 메시지를 브로드캐스트한다")
    void handle_success() {
        // when
        listener.handle(new StudyClosedEvent(12L, 45L));

        // then
        ArgumentCaptor<SystemClosedMessage> captor = ArgumentCaptor.forClass(SystemClosedMessage.class);
        verify(chatBroadcastPort).broadcast(eq("/sub/chat-rooms/12"), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("SYSTEM_CLOSED");
        assertThat(captor.getValue().message()).isEqualTo("스터디가 해산되어 채팅방이 종료되었습니다");
    }

    @Test
    @DisplayName("브로드캐스트 중 예외가 발생해도 전파되지 않고 로그만 남긴다")
    void handle_success_broadcastFailsSilently() {
        // given
        willThrow(new RuntimeException("connection error")).given(chatBroadcastPort).broadcast(any(String.class), any(Object.class));

        // when & then (예외 없이 정상 종료되어야 한다)
        listener.handle(new StudyClosedEvent(12L, 45L));
    }
}
