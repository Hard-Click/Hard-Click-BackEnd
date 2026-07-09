package com.wanted.backend.domain.chat.application.usecase;

import com.wanted.backend.domain.chat.application.result.ChatMessageListResult;

public interface ChatMessageQueryUseCase {
    ChatMessageListResult getMessages(Long chatRoomId, Long cursorId, int size, Long memberId);
}
