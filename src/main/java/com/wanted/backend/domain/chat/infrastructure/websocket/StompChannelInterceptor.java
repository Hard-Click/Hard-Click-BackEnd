package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.usecase.SocketTicketCommandUseCase;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_ROOM_DESTINATION_PATTERN = Pattern.compile("^/sub/chat-rooms/(\\d+)$");
    private static final String BEARER_PREFIX = "Bearer ";

    private final SocketTicketCommandUseCase socketTicketCommandUseCase;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    public StompChannelInterceptor(SocketTicketCommandUseCase socketTicketCommandUseCase,
                                   ChatRoomRepository chatRoomRepository,
                                   ChatRoomParticipantRepository chatRoomParticipantRepository) {
        this.socketTicketCommandUseCase = socketTicketCommandUseCase;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String ticket = extractTicket(accessor.getFirstNativeHeader("Authorization"));
        if (ticket == null) {
            throw new MessagingException("소켓 티켓이 없습니다.");
        }

        Long memberId = socketTicketCommandUseCase.consume(ticket)
                .orElseThrow(() -> new MessagingException("만료되었거나 이미 사용된 티켓입니다."));

        accessor.setUser(new ChatPrincipal(memberId));
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Long chatRoomId = extractChatRoomId(accessor.getDestination());
        if (chatRoomId == null) {
            return;
        }

        Long memberId = extractMemberId(accessor.getUser());
        if (memberId == null) {
            throw new MessagingException("인증되지 않은 연결입니다.");
        }

        if (chatRoomRepository.findById(chatRoomId).isEmpty()) {
            throw new MessagingException("존재하지 않는 채팅방입니다.");
        }

        if (!chatRoomParticipantRepository.existsByChatRoomIdAndMemberId(chatRoomId, memberId)) {
            throw new MessagingException("채팅방 참여자만 구독할 수 있습니다.");
        }
    }

    private String extractTicket(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private Long extractChatRoomId(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = CHAT_ROOM_DESTINATION_PATTERN.matcher(destination);
        return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
    }

    private Long extractMemberId(Principal principal) {
        return principal instanceof ChatPrincipal chatPrincipal ? chatPrincipal.getMemberId() : null;
    }
}
