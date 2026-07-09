package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.infrastructure.websocket.message.SystemClosedMessage;
import com.wanted.backend.domain.study.application.event.StudyClosedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class StudyClosedBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;

    public StudyClosedBroadcastListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StudyClosedEvent event) {
        try {
            messagingTemplate.convertAndSend(
                    "/sub/chat-rooms/" + event.chatRoomId(),
                    SystemClosedMessage.of());
        } catch (Exception e) {
            log.error("SYSTEM_CLOSED 브로드캐스트 실패 - chatRoomId={}, studyId={}", event.chatRoomId(), event.studyId(), e);
        }
    }
}
