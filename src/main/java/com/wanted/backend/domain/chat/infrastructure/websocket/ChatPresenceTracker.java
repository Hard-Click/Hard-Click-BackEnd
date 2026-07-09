package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.ParticipantPresenceMessage;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.PresenceUpdateMessage;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChatPresenceTracker {

    private static final Pattern CHAT_ROOM_DESTINATION_PATTERN = Pattern.compile("^/sub/chat-rooms/(\\d+)$");

    private final Map<Long, Set<Long>> onlineMembersByChatRoomId = new ConcurrentHashMap<>();
    private final Map<String, Long> chatRoomIdBySessionId = new ConcurrentHashMap<>();

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final MemberNamePort memberNamePort;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatPresenceTracker(ChatRoomParticipantRepository chatRoomParticipantRepository,
                               MemberNamePort memberNamePort,
                               SimpMessagingTemplate messagingTemplate) {
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
        this.memberNamePort = memberNamePort;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long chatRoomId = extractChatRoomId(accessor.getDestination());
        if (chatRoomId == null) {
            return;
        }
        Long memberId = extractMemberId(accessor.getUser());
        if (memberId == null) {
            return;
        }

        onlineMembersByChatRoomId.computeIfAbsent(chatRoomId, id -> ConcurrentHashMap.newKeySet()).add(memberId);
        chatRoomIdBySessionId.put(accessor.getSessionId(), chatRoomId);

        broadcastPresence(chatRoomId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Long chatRoomId = chatRoomIdBySessionId.remove(event.getSessionId());
        if (chatRoomId == null) {
            return;
        }
        Long memberId = extractMemberId(event.getUser());
        if (memberId == null) {
            return;
        }

        Set<Long> onlineMembers = onlineMembersByChatRoomId.get(chatRoomId);
        if (onlineMembers != null) {
            onlineMembers.remove(memberId);
        }

        broadcastPresence(chatRoomId);
    }

    public Set<Long> getOnlineMemberIds(Long chatRoomId) {
        return Set.copyOf(onlineMembersByChatRoomId.getOrDefault(chatRoomId, Set.of()));
    }

    private void broadcastPresence(Long chatRoomId) {
        List<Long> participantIds = chatRoomParticipantRepository.findMemberIdsByChatRoomId(chatRoomId);
        Set<Long> onlineMembers = onlineMembersByChatRoomId.getOrDefault(chatRoomId, Set.of());
        Map<Long, String> nameMap = resolveNameMap(participantIds);

        List<ParticipantPresenceMessage> participants = participantIds.stream()
                .map(memberId -> new ParticipantPresenceMessage(
                        memberId, maskName(nameMap.get(memberId)), onlineMembers.contains(memberId)))
                .toList();

        messagingTemplate.convertAndSend(
                "/sub/chat-rooms/" + chatRoomId,
                PresenceUpdateMessage.of(participants));
    }

    private Map<Long, String> resolveNameMap(Collection<Long> memberIds) {
        try {
            return memberNamePort.getNamesByMemberIds(memberIds);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "알 수 없음";
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
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
