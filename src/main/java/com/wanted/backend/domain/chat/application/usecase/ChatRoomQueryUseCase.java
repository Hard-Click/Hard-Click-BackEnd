package com.wanted.backend.domain.chat.application.usecase;

import com.wanted.backend.domain.chat.application.result.ChatRoomDetailResult;

public interface ChatRoomQueryUseCase {
    ChatRoomDetailResult getRoom(Long chatRoomId, Long memberId);
}
