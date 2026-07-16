package com.wanted.backend.domain.chat.infrastructure.persistence;

import com.wanted.backend.domain.chat.domain.model.ChatMessage;
import com.wanted.backend.domain.chat.domain.repository.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ChatMessageRepositoryAdapter implements ChatMessageRepository {

    private final SpringDataChatMessageRepository repository;

    public ChatMessageRepositoryAdapter(SpringDataChatMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        ChatMessageJpaEntity entity = new ChatMessageJpaEntity(
                chatMessage.getChatRoomId(), chatMessage.getSenderId(), chatMessage.getType(),
                chatMessage.getContent(), chatMessage.getSentAt()
        );
        ChatMessageJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ChatMessage> findByChatRoomIdBeforeCursor(Long chatRoomId, Long cursorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessageJpaEntity> entities = cursorId == null
                ? repository.findByChatRoomIdOrderByIdDesc(chatRoomId, pageable)
                : repository.findByChatRoomIdAndIdLessThanOrderByIdDesc(chatRoomId, cursorId, pageable);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ChatMessage> findLatestByChatRoomId(Long chatRoomId) {
        return repository.findFirstByChatRoomIdOrderByIdDesc(chatRoomId).map(this::toDomain);
    }

    @Override
    public long countUnreadByChatRoomIdAndMemberId(Long chatRoomId, Long memberId) {
        return repository.countUnreadByChatRoomIdAndMemberId(chatRoomId, memberId);
    }

    private ChatMessage toDomain(ChatMessageJpaEntity entity) {
        return ChatMessage.restore(entity.getId(), entity.getChatRoomId(), entity.getSenderId(),
                entity.getType(), entity.getContent(), entity.getSentAt());
    }
}
