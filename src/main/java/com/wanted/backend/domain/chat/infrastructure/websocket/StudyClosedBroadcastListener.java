package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.ChatBroadcastPort;
import com.wanted.backend.domain.chat.infrastructure.websocket.message.SystemClosedMessage;
import com.wanted.backend.domain.study.application.event.StudyClosedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class StudyClosedBroadcastListener {

    private final ChatBroadcastPort chatBroadcastPort;

    public StudyClosedBroadcastListener(ChatBroadcastPort chatBroadcastPort) {
        this.chatBroadcastPort = chatBroadcastPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StudyClosedEvent event) {
        try {
            chatBroadcastPort.broadcast(
                    "/sub/chat-rooms/" + event.chatRoomId(),
                    SystemClosedMessage.of());
        } catch (Exception e) {
            log.error("SYSTEM_CLOSED 브로드캐스트 실패 - chatRoomId={}, studyId={}", event.chatRoomId(), event.studyId(), e);
        }
    }
}
