package com.wanted.backend.domain.chat.domain.repository;

import com.wanted.backend.domain.chat.domain.model.ChatMessage;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage chatMessage);
}
