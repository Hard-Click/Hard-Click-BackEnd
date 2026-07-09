package com.wanted.backend.domain.chat.application.event;

import com.wanted.backend.domain.chat.domain.model.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessagePersistedEvent(
        Long chatRoomId,
        Long messageId,
        Long senderId,
        String content,
        LocalDateTime sentAt
) {
    public static ChatMessagePersistedEvent from(ChatMessage message) {
        return new ChatMessagePersistedEvent(
                message.getChatRoomId(), message.getId(), message.getSenderId(),
                message.getContent(), message.getSentAt());
    }
}
