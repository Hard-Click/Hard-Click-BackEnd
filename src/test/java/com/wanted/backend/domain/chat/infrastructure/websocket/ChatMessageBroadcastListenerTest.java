package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.event.ChatMessageEvent;
import com.wanted.backend.domain.chat.application.event.ChatMessagePersistedEvent;
import com.wanted.backend.domain.chat.application.port.ChatBroadcastPort;
import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessageBroadcastListenerTest {

    @InjectMocks
    private ChatMessageBroadcastListener listener;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private ChatBroadcastPort chatBroadcastPort;

    private ChatMessagePersistedEvent event() {
        return new ChatMessagePersistedEvent(45L, 500L, 1L, "안녕하세요", LocalDateTime.now());
    }

    @Test
    @DisplayName("이름 조회에 성공하면 마스킹된 이름과 함께 브로드캐스트된다")
    void handle_success() {
        // given
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of(1L, "이지연"));

        // when
        listener.handle(event());

        // then
        ArgumentCaptor<ChatMessageEvent> captor = ArgumentCaptor.forClass(ChatMessageEvent.class);
        verify(chatBroadcastPort).broadcast(eq("/sub/chat-rooms/45"), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("CHAT");
        assertThat(captor.getValue().messageId()).isEqualTo(500L);
        assertThat(captor.getValue().senderName()).isEqualTo("이*연");
        assertThat(captor.getValue().content()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("이름 조회가 예외를 던져도 알 수 없음으로 대체되어 브로드캐스트는 그대로 수행된다")
    void handle_success_nameResolutionFails() {
        // given
        given(memberNamePort.getNamesByMemberIds(any())).willThrow(new RuntimeException("timeout"));

        // when
        listener.handle(event());

        // then
        ArgumentCaptor<ChatMessageEvent> captor = ArgumentCaptor.forClass(ChatMessageEvent.class);
        verify(chatBroadcastPort).broadcast(eq("/sub/chat-rooms/45"), captor.capture());
        assertThat(captor.getValue().senderName()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("참여자 목록에 이름이 없으면 알 수 없음으로 대체된다")
    void handle_success_nameMissing() {
        // given
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of());

        // when
        listener.handle(event());

        // then
        ArgumentCaptor<ChatMessageEvent> captor = ArgumentCaptor.forClass(ChatMessageEvent.class);
        verify(chatBroadcastPort).broadcast(eq("/sub/chat-rooms/45"), captor.capture());
        assertThat(captor.getValue().senderName()).isEqualTo("알 수 없음");
    }
}
