package com.wanted.backend.domain.chat.application.result;

import java.time.LocalDateTime;

public record ChatMessageDetail(
        String type,
        Long messageId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime sentAt
) {}
