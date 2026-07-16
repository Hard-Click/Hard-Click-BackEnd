package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.event.ChatMessageEvent;
import com.wanted.backend.domain.chat.application.event.ChatMessagePersistedEvent;
import com.wanted.backend.domain.chat.application.port.ChatBroadcastPort;
import com.wanted.backend.domain.chat.application.port.MemberNamePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
public class ChatMessageBroadcastListener {

    private final MemberNamePort memberNamePort;
    private final ChatBroadcastPort chatBroadcastPort;

    public ChatMessageBroadcastListener(MemberNamePort memberNamePort, ChatBroadcastPort chatBroadcastPort) {
        this.memberNamePort = memberNamePort;
        this.chatBroadcastPort = chatBroadcastPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessagePersistedEvent event) {
        String senderName = maskName(resolveName(event.senderId()));

        chatBroadcastPort.broadcast(
                "/sub/chat-rooms/" + event.chatRoomId(),
                ChatMessageEvent.of(event, senderName));
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
