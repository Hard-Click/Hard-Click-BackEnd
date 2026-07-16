package com.wanted.backend.domain.chat.domain.model;

import java.time.LocalDateTime;

public class ChatRoomParticipant {

    private Long id;
    private Long chatRoomId;
    private Long memberId;
    private LocalDateTime joinedAt;
    private Long lastReadMessageId;

    private ChatRoomParticipant(Long id, Long chatRoomId, Long memberId, LocalDateTime joinedAt, Long lastReadMessageId) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.memberId = memberId;
        this.joinedAt = joinedAt;
        this.lastReadMessageId = lastReadMessageId;
    }

    public static ChatRoomParticipant create(Long chatRoomId, Long memberId) {
        return new ChatRoomParticipant(null, chatRoomId, memberId, LocalDateTime.now(), null);
    }

    public static ChatRoomParticipant restore(Long id, Long chatRoomId, Long memberId, LocalDateTime joinedAt,
                                               Long lastReadMessageId) {
        return new ChatRoomParticipant(id, chatRoomId, memberId, joinedAt, lastReadMessageId);
    }

    // notice_read_status처럼 메시지별 읽음 row를 쌓지 않고, 참여자당 "마지막으로 읽은 메시지 ID" 1개만
    // 전진시킨다(카카오톡 방식). 과거 메시지 ID로는 절대 역행하지 않는다.
    public void markRead(Long messageId) {
        if (messageId == null) {
            return;
        }
        if (lastReadMessageId == null || messageId > lastReadMessageId) {
            this.lastReadMessageId = messageId;
        }
    }

    public Long getId() { return id; }
    public Long getChatRoomId() { return chatRoomId; }
    public Long getMemberId() { return memberId; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public Long getLastReadMessageId() { return lastReadMessageId; }
}
