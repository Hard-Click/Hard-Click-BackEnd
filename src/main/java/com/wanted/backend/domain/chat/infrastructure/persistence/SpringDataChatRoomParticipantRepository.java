package com.wanted.backend.domain.chat.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipantJpaEntity, Long> {
    List<ChatRoomParticipantJpaEntity> findByChatRoomId(Long chatRoomId);

    boolean existsByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);
}
