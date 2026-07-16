package com.wanted.backend.domain.chat.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipantJpaEntity, Long> {
    List<ChatRoomParticipantJpaEntity> findByChatRoomId(Long chatRoomId);

    List<ChatRoomParticipantJpaEntity> findByMemberId(Long memberId);

    Optional<ChatRoomParticipantJpaEntity> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    boolean existsByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    void deleteByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    @Modifying
    @Query("UPDATE ChatRoomParticipantJpaEntity p SET p.lastReadMessageId = :messageId " +
            "WHERE p.chatRoomId = :chatRoomId AND p.memberId = :memberId " +
            "AND (p.lastReadMessageId IS NULL OR p.lastReadMessageId < :messageId)")
    int updateLastReadMessageId(@Param("chatRoomId") Long chatRoomId,
                                 @Param("memberId") Long memberId,
                                 @Param("messageId") Long messageId);
}
