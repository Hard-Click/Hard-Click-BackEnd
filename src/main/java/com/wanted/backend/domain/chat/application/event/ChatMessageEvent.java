package com.wanted.backend.domain.chat.application.event;

import com.wanted.backend.domain.chat.domain.model.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageEvent(
        String type,
        Long messageId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime sentAt
) {
    public static ChatMessageEvent of(ChatMessage message, String senderName) {
        return new ChatMessageEvent(
                "CHAT", message.getId(), message.getSenderId(), senderName,
                message.getContent(), message.getSentAt());
    }
}
