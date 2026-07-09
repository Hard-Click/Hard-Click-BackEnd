package com.wanted.backend.domain.chat.domain.repository;

import com.wanted.backend.domain.chat.domain.model.ChatMessage;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage chatMessage);

    List<ChatMessage> findByChatRoomIdBeforeCursor(Long chatRoomId, Long cursorId, int limit);

    Optional<ChatMessage> findLatestByChatRoomId(Long chatRoomId);
}
