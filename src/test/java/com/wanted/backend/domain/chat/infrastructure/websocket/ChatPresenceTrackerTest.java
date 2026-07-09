package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.ParticipantPresenceMessage;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.PresenceUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatPresenceTrackerTest {

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ChatPresenceTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new ChatPresenceTracker(chatRoomParticipantRepository, memberNamePort, messagingTemplate);
    }

    @Test
    @DisplayName("참여자가 채팅방을 구독하면 온라인으로 등록되고 PRESENCE_UPDATE가 브로드캐스트된다")
    void handleSubscribe_addsOnlineMemberAndBroadcasts() {
        // given
        given(chatRoomParticipantRepository.findMemberIdsByChatRoomId(45L)).willReturn(List.of(1L, 2L));
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of(1L, "이지연", 2L, "김민수"));

        // when
        tracker.handleSubscribe(new SessionSubscribeEvent(this, subscribeMessage("/sub/chat-rooms/45", "session-1", new ChatPrincipal(1L))));

        // then
        ArgumentCaptor<PresenceUpdateMessage> captor = ArgumentCaptor.forClass(PresenceUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat-rooms/45"), captor.capture());
        PresenceUpdateMessage message = captor.getValue();
        assertThat(message.type()).isEqualTo("PRESENCE_UPDATE");
        assertThat(message.participants()).containsExactlyInAnyOrder(
                new ParticipantPresenceMessage(1L, "이*연", true),
                new ParticipantPresenceMessage(2L, "김*수", false));
    }

    @Test
    @DisplayName("채팅방 채널이 아닌 구독은 무시한다")
    void handleSubscribe_nonChatRoomDestination_doesNothing() {
        // when
        tracker.handleSubscribe(new SessionSubscribeEvent(this, subscribeMessage("/sub/other-topic", "session-1", new ChatPrincipal(1L))));

        // then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("연결이 끊기면 해당 세션이 구독 중이던 방에서 제거되고 PRESENCE_UPDATE가 브로드캐스트된다")
    void handleDisconnect_removesOnlineMemberAndBroadcasts() {
        // given
        given(chatRoomParticipantRepository.findMemberIdsByChatRoomId(45L)).willReturn(List.of(1L));
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of(1L, "이지연"));
        tracker.handleSubscribe(new SessionSubscribeEvent(this, subscribeMessage("/sub/chat-rooms/45", "session-1", new ChatPrincipal(1L))));

        // when
        tracker.handleDisconnect(new SessionDisconnectEvent(this, disconnectMessage(), "session-1", CloseStatus.NORMAL, new ChatPrincipal(1L)));

        // then
        ArgumentCaptor<PresenceUpdateMessage> captor = ArgumentCaptor.forClass(PresenceUpdateMessage.class);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/sub/chat-rooms/45"), captor.capture());
        PresenceUpdateMessage lastMessage = captor.getAllValues().get(1);
        assertThat(lastMessage.participants()).containsExactly(new ParticipantPresenceMessage(1L, "이*연", false));
    }

    @Test
    @DisplayName("여러 참여자가 같은 방을 구독하면 모두 온라인으로 표시되고, 한 명이 연결 해제되어도 다른 참여자는 온라인으로 유지된다")
    void multipleMembers_oneDisconnects_othersStayOnline() {
        // given
        given(chatRoomParticipantRepository.findMemberIdsByChatRoomId(45L)).willReturn(List.of(1L, 2L));
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of(1L, "이지연", 2L, "김민수"));

        // when: 두 참여자가 각자 다른 세션으로 구독
        tracker.handleSubscribe(new SessionSubscribeEvent(this, subscribeMessage("/sub/chat-rooms/45", "session-1", new ChatPrincipal(1L))));
        tracker.handleSubscribe(new SessionSubscribeEvent(this, subscribeMessage("/sub/chat-rooms/45", "session-2", new ChatPrincipal(2L))));

        // when: 1L만 연결 해제
        tracker.handleDisconnect(new SessionDisconnectEvent(this, disconnectMessage(), "session-1", CloseStatus.NORMAL, new ChatPrincipal(1L)));

        // then: 구독 2회 + 연결 해제 1회, 총 3번 브로드캐스트되며 순서대로 상태가 반영된다
        ArgumentCaptor<PresenceUpdateMessage> captor = ArgumentCaptor.forClass(PresenceUpdateMessage.class);
        verify(messagingTemplate, times(3)).convertAndSend(eq("/sub/chat-rooms/45"), captor.capture());
        List<PresenceUpdateMessage> broadcasts = captor.getAllValues();

        // 1L 구독 시점: 1L만 온라인
        assertThat(broadcasts.get(0).participants()).containsExactlyInAnyOrder(
                new ParticipantPresenceMessage(1L, "이*연", true),
                new ParticipantPresenceMessage(2L, "김*수", false));

        // 2L 구독 시점: 둘 다 온라인
        assertThat(broadcasts.get(1).participants()).containsExactlyInAnyOrder(
                new ParticipantPresenceMessage(1L, "이*연", true),
                new ParticipantPresenceMessage(2L, "김*수", true));

        // 1L 연결 해제 시점: 2L은 여전히 온라인으로 유지된다
        assertThat(broadcasts.get(2).participants()).containsExactlyInAnyOrder(
                new ParticipantPresenceMessage(1L, "이*연", false),
                new ParticipantPresenceMessage(2L, "김*수", true));
    }

    @Test
    @DisplayName("추적되지 않은 세션의 연결 해제는 무시한다")
    void handleDisconnect_unknownSession_doesNothing() {
        // when
        tracker.handleDisconnect(new SessionDisconnectEvent(this, disconnectMessage(), "unknown-session", CloseStatus.NORMAL, new ChatPrincipal(1L)));

        // then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    private Message<byte[]> subscribeMessage(String destination, String sessionId, ChatPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionId(sessionId);
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> disconnectMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
