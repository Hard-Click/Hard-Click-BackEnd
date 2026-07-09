package com.wanted.backend.domain.chat.application.event;

import java.time.LocalDateTime;

public record ChatMessageEvent(
        String type,
        Long messageId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime sentAt
) {
    public static ChatMessageEvent of(ChatMessagePersistedEvent event, String senderName) {
        return new ChatMessageEvent(
                "CHAT", event.messageId(), event.senderId(), senderName,
                event.content(), event.sentAt());
    }
}
