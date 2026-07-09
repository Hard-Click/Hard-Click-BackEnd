package com.wanted.backend.domain.chat.application.result;

import java.time.LocalDateTime;

public record MyChatRoomDetail(
        Long chatRoomId,
        String name,
        String lastMessage,
        LocalDateTime lastMessageAt,
        int unreadCount
) {}
