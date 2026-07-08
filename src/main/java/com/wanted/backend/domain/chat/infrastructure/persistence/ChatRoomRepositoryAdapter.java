package com.wanted.backend.domain.chat.infrastructure.persistence;

import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRoomRepositoryAdapter implements ChatRoomRepository {

    private final SpringDataChatRoomRepository repository;

    public ChatRoomRepositoryAdapter(SpringDataChatRoomRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        ChatRoomJpaEntity entity = new ChatRoomJpaEntity(
                chatRoom.getStudyId(), chatRoom.getHostId(), chatRoom.getStatus(),
                chatRoom.getCreatedAt(), chatRoom.getUpdatedAt()
        );
        ChatRoomJpaEntity saved = repository.save(entity);
        return ChatRoom.restore(saved.getId(), saved.getStudyId(), saved.getHostId(),
                saved.getStatus(), saved.getCreatedAt(), saved.getUpdatedAt());
    }
}
