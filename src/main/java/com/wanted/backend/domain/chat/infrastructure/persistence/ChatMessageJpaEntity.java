package com.wanted.backend.domain.chat.infrastructure.persistence;

import com.wanted.backend.domain.chat.domain.model.ChatMessageType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Getter
public class ChatMessageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "sender_id")
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ChatMessageType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    protected ChatMessageJpaEntity() {}

    public ChatMessageJpaEntity(Long chatRoomId, Long senderId, ChatMessageType type, String content, LocalDateTime sentAt) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.type = type;
        this.content = content;
        this.sentAt = sentAt;
    }
}
