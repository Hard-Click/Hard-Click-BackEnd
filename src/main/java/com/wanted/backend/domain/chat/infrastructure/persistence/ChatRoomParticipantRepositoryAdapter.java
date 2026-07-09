package com.wanted.backend.domain.chat.infrastructure.persistence;

import com.wanted.backend.domain.chat.domain.model.ChatRoomParticipant;
import com.wanted.backend.domain.chat.domain.repository.ChatRoomParticipantRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    @Override
    public List<Long> findMemberIdsByChatRoomId(Long chatRoomId) {
        return repository.findByChatRoomId(chatRoomId).stream()
                .map(ChatRoomParticipantJpaEntity::getMemberId)
                .toList();
    }

    @Override
    public boolean existsByChatRoomIdAndMemberId(Long chatRoomId, Long memberId) {
        return repository.existsByChatRoomIdAndMemberId(chatRoomId, memberId);
    }

    @Override
    public void deleteByChatRoomIdAndMemberId(Long chatRoomId, Long memberId) {
        repository.deleteByChatRoomIdAndMemberId(chatRoomId, memberId);
    }
}
