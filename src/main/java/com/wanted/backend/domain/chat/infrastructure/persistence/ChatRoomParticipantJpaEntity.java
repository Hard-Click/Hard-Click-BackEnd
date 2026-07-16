package com.wanted.backend.domain.chat.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_participant")
@Getter
public class ChatRoomParticipantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_participant_id")
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    protected ChatRoomParticipantJpaEntity() {}

    public ChatRoomParticipantJpaEntity(Long id, Long chatRoomId, Long memberId, LocalDateTime joinedAt, Long lastReadMessageId) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.memberId = memberId;
        this.joinedAt = joinedAt;
        this.lastReadMessageId = lastReadMessageId;
    }
}
