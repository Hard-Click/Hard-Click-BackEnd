package com.wanted.backend.domain.chat.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataChatMessageRepository extends JpaRepository<ChatMessageJpaEntity, Long> {
    List<ChatMessageJpaEntity> findByChatRoomIdOrderByIdDesc(Long chatRoomId, Pageable pageable);

    List<ChatMessageJpaEntity> findByChatRoomIdAndIdLessThanOrderByIdDesc(Long chatRoomId, Long cursorId, Pageable pageable);
}
