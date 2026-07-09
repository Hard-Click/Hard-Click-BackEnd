package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.repository.ChatMessageRepository;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.ParticipantPresenceMessage;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.SystemLeaveMessage;
import com.wanted.backend.domain.study.application.event.StudyLeftEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class StudyLeftBroadcastListener {

    private final MemberNamePort memberNamePort;
    private final ChatParticipantPresenceResolver presenceResolver;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public StudyLeftBroadcastListener(MemberNamePort memberNamePort,
                                      ChatParticipantPresenceResolver presenceResolver,
                                      ChatMessageRepository chatMessageRepository,
                                      SimpMessagingTemplate messagingTemplate) {
        this.memberNamePort = memberNamePort;
        this.presenceResolver = presenceResolver;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StudyLeftEvent event) {
        try {
            List<ParticipantPresenceMessage> participants = presenceResolver.resolve(event.chatRoomId());
            String leaverName = maskName(resolveName(event.memberId()));
            String message = leaverName + "님이 퇴장했습니다";

            chatMessageRepository.save(ChatMessage.createSystemLeave(event.chatRoomId(), message));

            messagingTemplate.convertAndSend(
                    "/sub/chat-rooms/" + event.chatRoomId(),
                    SystemLeaveMessage.of(message, participants));
        } catch (Exception e) {
            log.error("SYSTEM_LEAVE 브로드캐스트 실패 - chatRoomId={}, memberId={}", event.chatRoomId(), event.memberId(), e);
        }
    }

    private String resolveName(Long memberId) {
        try {
            return memberNamePort.getNamesByMemberIds(Set.of(memberId)).get(memberId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "알 수 없음";
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }
}
