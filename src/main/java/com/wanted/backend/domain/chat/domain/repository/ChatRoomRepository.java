package com.wanted.backend.domain.chat.domain.repository;

import com.wanted.backend.domain.chat.domain.model.ChatRoom;

public interface ChatRoomRepository {
    ChatRoom save(ChatRoom chatRoom);
}
