package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.command.MarkChatRoomReadCommand;
import com.wanted.backend.domain.chat.application.usecase.ChatRoomCommandUseCase;
import com.wanted.backend.domain.chat.application.usecase.SocketTicketCommandUseCase;
import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.repository.ChatMessageRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompChannelInterceptor.class);

    private static final Pattern CHAT_ROOM_DESTINATION_PATTERN = Pattern.compile("^/sub/chat-rooms/(\\d+)$");
    private static final String BEARER_PREFIX = "Bearer ";

    private final SocketTicketCommandUseCase socketTicketCommandUseCase;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomCommandUseCase chatRoomCommandUseCase;
    private final Executor chatReadExecutor;

    public StompChannelInterceptor(SocketTicketCommandUseCase socketTicketCommandUseCase,
                                   ChatRoomRepository chatRoomRepository,
                                   ChatRoomParticipantRepository chatRoomParticipantRepository,
                                   ChatMessageRepository chatMessageRepository,
                                   ChatRoomCommandUseCase chatRoomCommandUseCase,
                                   @Qualifier("chatReadExecutor") Executor chatReadExecutor) {
        this.socketTicketCommandUseCase = socketTicketCommandUseCase;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomCommandUseCase = chatRoomCommandUseCase;
        this.chatReadExecutor = chatReadExecutor;
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

        markReadOnSubscribe(chatRoomId, memberId);
    }

    // 방을 구독한다 = 지금 그 방을 보고 있다고 간주하고, 별도의 "읽음 처리" API 없이
    // 최신 메시지까지 자동으로 읽음 처리한다. 읽음 갱신 실패가 구독 자체를 막으면 안 되므로
    // 부가 기능으로 취급해 예외를 삼킨다(백그라운드 탭에 열어만 둬도 읽음 처리되는 트레이드오프는
    // 감수한 설계 — 문서 참고).
    // preSend는 한정된 clientInboundChannel 스레드 풀에서 동기 실행되므로, DB I/O는 전용
    // chatReadExecutor로 격리해 STOMP 스레드가 즉시 반환되도록 한다.
    private void markReadOnSubscribe(Long chatRoomId, Long memberId) {
        chatReadExecutor.execute(() -> {
            try {
                chatMessageRepository.findLatestByChatRoomId(chatRoomId)
                        .map(ChatMessage::getId)
                        .ifPresent(latestMessageId -> chatRoomCommandUseCase.markRead(
                                new MarkChatRoomReadCommand(chatRoomId, memberId, latestMessageId)));
            } catch (Exception e) {
                log.warn("구독 시점 자동 읽음 처리 실패. chatRoomId={}, memberId={}", chatRoomId, memberId, e);
            }
        });
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
