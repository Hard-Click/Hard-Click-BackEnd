package com.wanted.backend.domain.chat.domain.repository;

import com.wanted.backend.domain.chat.domain.model.ChatRoomParticipant;

public interface ChatRoomParticipantRepository {
    ChatRoomParticipant save(ChatRoomParticipant chatRoomParticipant);
}
