package com.wanted.backend.domain.chat.domain.repository;

import com.wanted.backend.domain.chat.domain.model.ChatRoomParticipant;

import java.util.List;

public interface ChatRoomParticipantRepository {
    ChatRoomParticipant save(ChatRoomParticipant chatRoomParticipant);

    List<Long> findMemberIdsByChatRoomId(Long chatRoomId);

    List<Long> findChatRoomIdsByMemberId(Long memberId);

    boolean existsByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    void deleteByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    // 참여자당 "마지막으로 읽은 메시지 ID"를 원자적으로 전진시킨다. 과거 값으로의 역행은
    // 조건절(WHERE ... last_read_message_id < :messageId)로 DB 레벨에서 막아, 여러 세션이
    // 동시에 구독을 열어도 read-modify-write 경쟁 없이 안전하다.
    void updateLastReadMessageId(Long chatRoomId, Long memberId, Long messageId);
}
