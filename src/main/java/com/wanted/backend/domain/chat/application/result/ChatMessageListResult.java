package com.wanted.backend.domain.chat.application.result;

import java.util.List;

public record ChatMessageListResult(
        List<ChatMessageDetail> messages,
        boolean hasNext,
        Long nextCursorId
) {}
