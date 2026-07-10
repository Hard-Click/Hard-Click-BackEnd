package com.wanted.backend.domain.chat.domain.repository;

import com.wanted.backend.domain.chat.domain.model.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository {
    ChatRoom save(ChatRoom chatRoom);

    Optional<ChatRoom> findById(Long id);

    Optional<ChatRoom> findByStudyId(Long studyId);

    List<ChatRoom> findAllByIdIn(List<Long> ids);
}
