package com.wanted.backend.domain.chat.infrastructure.persistence;

import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.repository.ChatMessageRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ChatMessageRepositoryAdapter implements ChatMessageRepository {

    private final SpringDataChatMessageRepository repository;

    public ChatMessageRepositoryAdapter(SpringDataChatMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        ChatMessageJpaEntity entity = new ChatMessageJpaEntity(
                chatMessage.getChatRoomId(), chatMessage.getSenderId(),
                chatMessage.getContent(), chatMessage.getSentAt()
        );
        ChatMessageJpaEntity saved = repository.save(entity);
        return ChatMessage.restore(saved.getId(), saved.getChatRoomId(), saved.getSenderId(),
                saved.getContent(), saved.getSentAt());
    }
}
