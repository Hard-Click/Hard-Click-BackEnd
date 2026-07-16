package com.wanted.backend.domain.chat.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataChatMessageRepository extends JpaRepository<ChatMessageJpaEntity, Long> {
    List<ChatMessageJpaEntity> findByChatRoomIdOrderByIdDesc(Long chatRoomId, Pageable pageable);

    List<ChatMessageJpaEntity> findByChatRoomIdAndIdLessThanOrderByIdDesc(Long chatRoomId, Long cursorId, Pageable pageable);

    Optional<ChatMessageJpaEntity> findFirstByChatRoomIdOrderByIdDesc(Long chatRoomId);

    // notice_read_status처럼 (메시지, 참여자)마다 읽음 row를 두지 않고, 참여자의 last_read_message_id
    // 포인터 하나와 조인해서 그보다 뒤에 온 메시지 수만 센다. chat_room_participant는 이미
    // uk_chat_room_participant_room_member(chat_room_id, member_id) 유니크 인덱스로 그 참여자 행을
    // 즉시 찾고, chat_message 쪽은 idx_chat_message_room_id(chat_room_id, chat_message_id) 복합
    // 인덱스로 range count가 인덱스만으로 처리된다.
    @Query(value = "SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_room_participant p ON p.chat_room_id = m.chat_room_id " +
            "WHERE p.chat_room_id = :chatRoomId AND p.member_id = :memberId " +
            "AND (p.last_read_message_id IS NULL OR m.chat_message_id > p.last_read_message_id)",
            nativeQuery = true)
    long countUnreadByChatRoomIdAndMemberId(@Param("chatRoomId") Long chatRoomId, @Param("memberId") Long memberId);
}
