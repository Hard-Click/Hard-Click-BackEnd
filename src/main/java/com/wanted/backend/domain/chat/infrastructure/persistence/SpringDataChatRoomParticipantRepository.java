package com.wanted.backend.domain.chat.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipantJpaEntity, Long> {
}
