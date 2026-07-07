package com.wanted.backend.domain.chat.domain.model;

import java.time.LocalDateTime;

public class ChatRoomParticipant {

    private Long id;
    private Long chatRoomId;
    private Long memberId;
    private LocalDateTime joinedAt;

    private ChatRoomParticipant(Long id, Long chatRoomId, Long memberId, LocalDateTime joinedAt) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.memberId = memberId;
        this.joinedAt = joinedAt;
    }

    public static ChatRoomParticipant create(Long chatRoomId, Long memberId) {
        return new ChatRoomParticipant(null, chatRoomId, memberId, LocalDateTime.now());
    }

    public static ChatRoomParticipant restore(Long id, Long chatRoomId, Long memberId, LocalDateTime joinedAt) {
        return new ChatRoomParticipant(id, chatRoomId, memberId, joinedAt);
    }

    public Long getId() { return id; }
    public Long getChatRoomId() { return chatRoomId; }
    public Long getMemberId() { return memberId; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
