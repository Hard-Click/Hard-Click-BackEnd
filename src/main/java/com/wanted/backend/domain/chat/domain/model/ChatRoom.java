package com.wanted.backend.domain.chat.domain.model;

import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;

import java.time.LocalDateTime;

public class ChatRoom {

    private Long id;
    private Long studyId;
    private Long hostId;
    private ChatRoomStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ChatRoom(Long id, Long studyId, Long hostId, ChatRoomStatus status,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studyId = studyId;
        this.hostId = hostId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ChatRoom create(Long studyId, Long hostId) {
        LocalDateTime now = LocalDateTime.now();
        return new ChatRoom(null, studyId, hostId, ChatRoomStatus.ACTIVE, now, now);
    }

    public static ChatRoom restore(Long id, Long studyId, Long hostId, ChatRoomStatus status,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ChatRoom(id, studyId, hostId, status, createdAt, updatedAt);
    }

    public void validateActive() {
        if (status != ChatRoomStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_CLOSED);
        }
    }

    public Long getId() { return id; }
    public Long getStudyId() { return studyId; }
    public Long getHostId() { return hostId; }
    public ChatRoomStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
