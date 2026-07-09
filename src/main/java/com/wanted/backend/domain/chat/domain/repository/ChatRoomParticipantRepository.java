package com.wanted.backend.domain.chat.domain.repository;

import com.wanted.backend.domain.chat.domain.model.ChatRoomParticipant;

import java.util.List;

public interface ChatRoomParticipantRepository {
    ChatRoomParticipant save(ChatRoomParticipant chatRoomParticipant);

    List<Long> findMemberIdsByChatRoomId(Long chatRoomId);

    List<Long> findChatRoomIdsByMemberId(Long memberId);

    boolean existsByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    void deleteByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);
}
