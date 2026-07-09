package com.wanted.backend.domain.chat.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataChatRoomRepository extends JpaRepository<ChatRoomJpaEntity, Long> {
    Optional<ChatRoomJpaEntity> findByStudyId(Long studyId);
}
