package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.ChatBroadcastPort;
import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.ParticipantPresenceMessage;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.PresenceUpdateMessage;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// ASG로 여러 인스턴스가 뜨면 "누가 접속 중인지"도 인스턴스 로컬 메모리에만 있으면 안 되므로,
// StringRedisTemplate으로 온라인 상태 자체를 인스턴스 간에 공유한다.
// 참고로 크래시 등으로 정상 disconnect 이벤트 없이 프로세스가 죽으면 room set에 유령 온라인
// 상태가 남을 수 있다(TTL/heartbeat 미적용) — cross-instance 브로드캐스트 수정이 우선이라
// 이번 스코프에는 넣지 않았고, 별도 후속 작업으로 남겨둔다.
@Component
public class ChatPresenceTracker {

    private static final Pattern CHAT_ROOM_DESTINATION_PATTERN = Pattern.compile("^/sub/chat-rooms/(\\d+)$");
    private static final String ROOM_KEY_PREFIX = "chat:presence:room:";
    private static final String SESSION_KEY_PREFIX = "chat:presence:session:";

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final MemberNamePort memberNamePort;
    private final ChatBroadcastPort chatBroadcastPort;
    private final StringRedisTemplate redisTemplate;

    public ChatPresenceTracker(ChatRoomParticipantRepository chatRoomParticipantRepository,
                               MemberNamePort memberNamePort,
                               ChatBroadcastPort chatBroadcastPort,
                               StringRedisTemplate redisTemplate) {
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
        this.memberNamePort = memberNamePort;
        this.chatBroadcastPort = chatBroadcastPort;
        this.redisTemplate = redisTemplate;
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

        redisTemplate.opsForSet().add(roomKey(chatRoomId), memberId.toString());
        redisTemplate.opsForValue().set(sessionKey(accessor.getSessionId()), chatRoomId.toString());

        broadcastPresence(chatRoomId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionKey = sessionKey(event.getSessionId());
        String chatRoomIdValue = redisTemplate.opsForValue().get(sessionKey);
        if (chatRoomIdValue == null) {
            return;
        }
        redisTemplate.delete(sessionKey);

        Long memberId = extractMemberId(event.getUser());
        if (memberId == null) {
            return;
        }

        Long chatRoomId = Long.valueOf(chatRoomIdValue);
        redisTemplate.opsForSet().remove(roomKey(chatRoomId), memberId.toString());

        broadcastPresence(chatRoomId);
    }

    public Set<Long> getOnlineMemberIds(Long chatRoomId) {
        Set<String> members = redisTemplate.opsForSet().members(roomKey(chatRoomId));
        if (members == null || members.isEmpty()) {
            return Set.of();
        }
        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }

    private void broadcastPresence(Long chatRoomId) {
        List<Long> participantIds = chatRoomParticipantRepository.findMemberIdsByChatRoomId(chatRoomId);
        Set<Long> onlineMembers = getOnlineMemberIds(chatRoomId);
        Map<Long, String> nameMap = resolveNameMap(participantIds);

        List<ParticipantPresenceMessage> participants = participantIds.stream()
                .map(memberId -> new ParticipantPresenceMessage(
                        memberId, maskName(nameMap.get(memberId)), onlineMembers.contains(memberId)))
                .toList();

        chatBroadcastPort.broadcast(
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

    private String roomKey(Long chatRoomId) {
        return ROOM_KEY_PREFIX + chatRoomId;
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }
}
