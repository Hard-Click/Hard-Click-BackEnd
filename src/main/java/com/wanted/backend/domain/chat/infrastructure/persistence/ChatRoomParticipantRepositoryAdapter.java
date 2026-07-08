package com.wanted.backend.domain.chat.infrastructure.persistence;

import com.wanted.backend.domain.chat.domain.model.ChatRoomParticipant;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRoomParticipantRepositoryAdapter implements ChatRoomParticipantRepository {

    private final SpringDataChatRoomParticipantRepository repository;

    public ChatRoomParticipantRepositoryAdapter(SpringDataChatRoomParticipantRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatRoomParticipant save(ChatRoomParticipant chatRoomParticipant) {
        ChatRoomParticipantJpaEntity entity = new ChatRoomParticipantJpaEntity(
                chatRoomParticipant.getChatRoomId(), chatRoomParticipant.getMemberId(), chatRoomParticipant.getJoinedAt()
        );
        ChatRoomParticipantJpaEntity saved = repository.save(entity);
        return ChatRoomParticipant.restore(saved.getId(), saved.getChatRoomId(), saved.getMemberId(), saved.getJoinedAt());
    }
}
