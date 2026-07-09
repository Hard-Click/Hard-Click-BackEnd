package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.ParticipantPresenceMessage;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ChatParticipantPresenceResolver {

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final MemberNamePort memberNamePort;
    private final ChatPresenceTracker presenceTracker;

    public ChatParticipantPresenceResolver(ChatRoomParticipantRepository chatRoomParticipantRepository,
                                           MemberNamePort memberNamePort,
                                           ChatPresenceTracker presenceTracker) {
        this.chatRoomParticipantRepository = chatRoomParticipantRepository;
        this.memberNamePort = memberNamePort;
        this.presenceTracker = presenceTracker;
    }

    public List<ParticipantPresenceMessage> resolve(Long chatRoomId) {
        List<Long> participantIds = chatRoomParticipantRepository.findMemberIdsByChatRoomId(chatRoomId);
        Set<Long> onlineMembers = presenceTracker.getOnlineMemberIds(chatRoomId);
        Map<Long, String> nameMap = resolveNameMap(participantIds);

        return participantIds.stream()
                .map(memberId -> new ParticipantPresenceMessage(
                        memberId, maskName(nameMap.get(memberId)), onlineMembers.contains(memberId)))
                .toList();
    }

    private Map<Long, String> resolveNameMap(Collection<Long> memberIds) {
        try {
            return memberNamePort.getNamesByMemberIds(memberIds);
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "알 수 없음";
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }
}
