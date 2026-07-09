package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.usecase.SocketTicketCommandUseCase;
import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.model.ChatRoomStatus;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StompChannelInterceptorTest {

    @Mock
    private SocketTicketCommandUseCase socketTicketCommandUseCase;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    private StompChannelInterceptor interceptor;

    private ChatRoom activeChatRoom() {
        return ChatRoom.restore(45L, 100L, 1L, ChatRoomStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("유효한 티켓으로 CONNECT하면 Principal이 바인딩된다")
    void connect_success() {
        interceptor = new StompChannelInterceptor(socketTicketCommandUseCase, chatRoomRepository, chatRoomParticipantRepository);
        given(socketTicketCommandUseCase.consume("valid-ticket")).willReturn(Optional.of(1L));

        Message<?> result = interceptor.preSend(connectMessage("Bearer valid-ticket"), null);

        StompHeaderAccessor resultAccessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(resultAccessor.getUser()).isInstanceOf(ChatPrincipal.class);
        assertThat(((ChatPrincipal) resultAccessor.getUser()).getMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 CONNECT가 거부된다")
    void connect_fail_noTicket() {
        interceptor = new StompChannelInterceptor(socketTicketCommandUseCase, chatRoomRepository, chatRoomParticipantRepository);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage(null), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("만료되었거나 이미 사용된 티켓이면 CONNECT가 거부된다")
    void connect_fail_invalidTicket() {
        interceptor = new StompChannelInterceptor(socketTicketCommandUseCase, chatRoomRepository, chatRoomParticipantRepository);
        given(socketTicketCommandUseCase.consume("used-ticket")).willReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer used-ticket"), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("참여자가 채팅방을 구독하면 정상 통과한다")
    void subscribe_success() {
        interceptor = new StompChannelInterceptor(socketTicketCommandUseCase, chatRoomRepository, chatRoomParticipantRepository);
        given(chatRoomRepository.findById(45L)).willReturn(Optional.of(activeChatRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(45L, 1L)).willReturn(true);

        Message<?> result = interceptor.preSend(subscribeMessage("/sub/chat-rooms/45", new ChatPrincipal(1L)), null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("채팅방 채널이 아닌 구독은 검증 없이 통과한다")
    void subscribe_success_nonChatRoomDestination() {
        interceptor = new StompChannelInterceptor(socketTicketCommandUseCase, chatRoomRepository, chatRoomParticipantRepository);

        Message<?> result = interceptor.preSend(subscribeMessage("/sub/other-topic", new ChatPrincipal(1L)), null);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("인증되지 않은 연결이 구독을 시도하면 거부된다")
    void subscribe_fail_notAuthenticated() {
        interceptor = new StompChannelInterceptor(socketTicketCommandUseCase, chatRoomRepository, chatRoomParticipantRepository);

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/sub/chat-rooms/45", null), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("존재하지 않는 채팅방을 구독하면 거부된다")
    void subscribe_fail_roomNotFound() {
        interceptor = new StompChannelInterceptor(socketTicketCommandUseCase, chatRoomRepository, chatRoomParticipantRepository);
        given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/sub/chat-rooms/999", new ChatPrincipal(1L)), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("참여자가 아닌 회원이 구독하면 거부된다 (강퇴된 회원 포함)")
    void subscribe_fail_notParticipant() {
        interceptor = new StompChannelInterceptor(socketTicketCommandUseCase, chatRoomRepository, chatRoomParticipantRepository);
        given(chatRoomRepository.findById(45L)).willReturn(Optional.of(activeChatRoom()));
        given(chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(45L, 999L)).willReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/sub/chat-rooms/45", new ChatPrincipal(999L)), null))
                .isInstanceOf(MessagingException.class);
    }

    private Message<byte[]> connectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(String destination, ChatPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionId("session-1");
        if (principal != null) {
            accessor.setUser(principal);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
