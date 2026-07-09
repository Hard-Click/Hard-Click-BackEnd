package com.wanted.backend.domain.chat.infrastructure.persistence;

import com.wanted.backend.domain.chat.domain.model.ChatRoom;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ChatRoomRepositoryAdapter implements ChatRoomRepository {

    private final SpringDataChatRoomRepository repository;

    public ChatRoomRepositoryAdapter(SpringDataChatRoomRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        ChatRoomJpaEntity entity = new ChatRoomJpaEntity(
                chatRoom.getId(), chatRoom.getStudyId(), chatRoom.getHostId(), chatRoom.getStatus(),
                chatRoom.getCreatedAt(), chatRoom.getUpdatedAt()
        );
        ChatRoomJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ChatRoom> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ChatRoom> findByStudyId(Long studyId) {
        return repository.findByStudyId(studyId).map(this::toDomain);
    }

    private ChatRoom toDomain(ChatRoomJpaEntity entity) {
        return ChatRoom.restore(entity.getId(), entity.getStudyId(), entity.getHostId(),
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
