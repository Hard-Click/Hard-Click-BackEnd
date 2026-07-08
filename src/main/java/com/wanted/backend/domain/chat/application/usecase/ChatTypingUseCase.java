package com.wanted.backend.domain.chat.application.usecase;

public interface ChatTypingUseCase {
    void notifyTyping(Long chatRoomId, Long memberId);
}
