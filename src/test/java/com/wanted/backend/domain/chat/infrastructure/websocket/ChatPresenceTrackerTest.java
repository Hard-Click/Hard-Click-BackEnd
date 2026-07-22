package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.ChatBroadcastPort;
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
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// 온라인 상태는 ASG 인스턴스 간에 공유되어야 해서 StringRedisTemplate(Set/Value)로 옮겼다.
// Mockito는 시퀀스 상태(구독 여러 번 → 연결 해제)를 그대로 stub하기 어려우므로,
// opsForSet/opsForValue를 이 테스트 안에서만 쓰는 작은 인메모리 페이크로 backing한다.
@ExtendWith(MockitoExtension.class)
class ChatPresenceTrackerTest {

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Mock
    private MemberNamePort memberNamePort;

    @Mock
    private ChatBroadcastPort chatBroadcastPort;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final Map<String, Set<String>> fakeSets = new HashMap<>();
    private final Map<String, String> fakeValues = new HashMap<>();

    private ChatPresenceTracker tracker;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        lenient().when(setOperations.add(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            fakeSets.computeIfAbsent(key, k -> new HashSet<>()).add(value);
            return 1L;
        });
        lenient().when(setOperations.remove(anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = (String) invocation.getArgument(1);
            Set<String> set = fakeSets.get(key);
            return set != null && set.remove(value) ? 1L : 0L;
        });
        lenient().when(setOperations.members(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return new HashSet<>(fakeSets.getOrDefault(key, Set.of()));
        });
        lenient().when(valueOperations.get(anyString())).thenAnswer(invocation ->
                fakeValues.get((String) invocation.getArgument(0)));
        lenient().doAnswer(invocation -> {
            fakeValues.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString());
        lenient().when(redisTemplate.delete(anyString())).thenAnswer(invocation ->
                fakeValues.remove((String) invocation.getArgument(0)) != null);

        tracker = new ChatPresenceTracker(chatRoomParticipantRepository, memberNamePort, chatBroadcastPort, redisTemplate);
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
        verify(chatBroadcastPort).broadcast(eq("/sub/chat-rooms/45"), captor.capture());
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
        verify(chatBroadcastPort, never()).broadcast(anyString(), any(Object.class));
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
        verify(chatBroadcastPort, times(2)).broadcast(eq("/sub/chat-rooms/45"), captor.capture());
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
        verify(chatBroadcastPort, times(3)).broadcast(eq("/sub/chat-rooms/45"), captor.capture());
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
    @DisplayName("구독 처리 중 Redis 장애가 나도 예외를 삼키고 전파하지 않는다")
    void handleSubscribe_redisFails_doesNotPropagate() {
        // given
        willThrow(new RuntimeException("redis timeout")).given(setOperations).add(anyString(), anyString());

        // when & then
        assertThatCode(() -> tracker.handleSubscribe(
                new SessionSubscribeEvent(this, subscribeMessage("/sub/chat-rooms/45", "session-1", new ChatPrincipal(1L)))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("연결 해제 처리 중 Redis 장애가 나도 예외를 삼키고 전파하지 않는다")
    void handleDisconnect_redisFails_doesNotPropagate() {
        // given
        given(chatRoomParticipantRepository.findMemberIdsByChatRoomId(45L)).willReturn(List.of(1L));
        given(memberNamePort.getNamesByMemberIds(any())).willReturn(Map.of(1L, "이지연"));
        tracker.handleSubscribe(new SessionSubscribeEvent(this, subscribeMessage("/sub/chat-rooms/45", "session-1", new ChatPrincipal(1L))));
        willThrow(new RuntimeException("redis timeout")).given(setOperations).remove(anyString(), any());

        // when & then
        assertThatCode(() -> tracker.handleDisconnect(
                new SessionDisconnectEvent(this, disconnectMessage(), "session-1", CloseStatus.NORMAL, new ChatPrincipal(1L))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("추적되지 않은 세션의 연결 해제는 무시한다")
    void handleDisconnect_unknownSession_doesNothing() {
        // when
        tracker.handleDisconnect(new SessionDisconnectEvent(this, disconnectMessage(), "unknown-session", CloseStatus.NORMAL, new ChatPrincipal(1L)));

        // then
        verify(chatBroadcastPort, never()).broadcast(anyString(), any(Object.class));
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
