package com.wanted.backend.domain.chat.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataChatMessageRepository extends JpaRepository<ChatMessageJpaEntity, Long> {
    List<ChatMessageJpaEntity> findByChatRoomIdOrderByIdDesc(Long chatRoomId, Pageable pageable);

    List<ChatMessageJpaEntity> findByChatRoomIdAndIdLessThanOrderByIdDesc(Long chatRoomId, Long cursorId, Pageable pageable);

    Optional<ChatMessageJpaEntity> findFirstByChatRoomIdOrderByIdDesc(Long chatRoomId);

    // 미읽음 = "남이 보낸 안 읽은 것"만이므로 조회자 본인이 보낸 메시지는 제외한다(#583).
    // sender_id <> :senderId 조건은 NULL 행을 함께 걸러내므로, 시스템 메시지(sender 없음)도
    // 카운팅되지 않는다 — 일반 메신저와 동일한 의도된 동작.
    long countByChatRoomIdAndSenderIdNot(Long chatRoomId, Long senderId);

    long countByChatRoomIdAndIdGreaterThanAndSenderIdNot(Long chatRoomId, Long id, Long senderId);
}
