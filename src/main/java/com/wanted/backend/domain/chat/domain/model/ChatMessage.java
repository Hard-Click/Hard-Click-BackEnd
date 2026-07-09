package com.wanted.backend.domain.chat.domain.model;

import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;

import java.time.LocalDateTime;

public class ChatMessage {

    private static final int MAX_CONTENT_LENGTH = 1000;

    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private ChatMessageType type;
    private String content;
    private LocalDateTime sentAt;

    private ChatMessage(Long id, Long chatRoomId, Long senderId, ChatMessageType type, String content, LocalDateTime sentAt) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.type = type;
        this.content = content;
        this.sentAt = sentAt;
    }

    public static ChatMessage create(Long chatRoomId, Long senderId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_CONTENT_REQUIRED);
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_CONTENT_TOO_LONG);
        }
        return new ChatMessage(null, chatRoomId, senderId, ChatMessageType.CHAT, content, LocalDateTime.now());
    }

    public static ChatMessage createSystemJoin(Long chatRoomId, String content) {
        return new ChatMessage(null, chatRoomId, null, ChatMessageType.SYSTEM_JOIN, content, LocalDateTime.now());
    }

    public static ChatMessage createSystemLeave(Long chatRoomId, String content) {
        return new ChatMessage(null, chatRoomId, null, ChatMessageType.SYSTEM_LEAVE, content, LocalDateTime.now());
    }

    public static ChatMessage restore(Long id, Long chatRoomId, Long senderId, ChatMessageType type, String content, LocalDateTime sentAt) {
        return new ChatMessage(id, chatRoomId, senderId, type, content, sentAt);
    }

    public Long getId() { return id; }
    public Long getChatRoomId() { return chatRoomId; }
    public Long getSenderId() { return senderId; }
    public ChatMessageType getType() { return type; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
}
