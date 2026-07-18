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
    private final SpringDataChatRoomParticipantRepository participantRepository;

    public ChatMessageRepositoryAdapter(SpringDataChatMessageRepository repository,
                                         SpringDataChatRoomParticipantRepository participantRepository) {
        this.repository = repository;
        this.participantRepository = participantRepository;
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

    // native @Query JOIN 대신, 참여자의 last_read_message_id 포인터를 먼저 조회한 뒤
    // 파생 쿼리로 나눠서 센다. 조회자 본인이 보낸 메시지는 미읽음이 아니므로 제외한다(#583).
    // chat_room_participant는 uk_chat_room_participant_room_member 유니크 인덱스로 즉시 찾고,
    // chat_message 쪽은 idx_chat_message_room_id(chat_room_id, chat_message_id) 복합 인덱스를 탄다.
    @Override
    public long countUnreadByChatRoomIdAndMemberId(Long chatRoomId, Long memberId) {
        Long lastReadMessageId = participantRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .map(ChatRoomParticipantJpaEntity::getLastReadMessageId)
                .orElse(null);

        return lastReadMessageId == null
                ? repository.countByChatRoomIdAndSenderIdNot(chatRoomId, memberId)
                : repository.countByChatRoomIdAndIdGreaterThanAndSenderIdNot(chatRoomId, lastReadMessageId, memberId);
    }

    private ChatMessage toDomain(ChatMessageJpaEntity entity) {
        return ChatMessage.restore(entity.getId(), entity.getChatRoomId(), entity.getSenderId(),
                entity.getType(), entity.getContent(), entity.getSentAt());
    }
}
