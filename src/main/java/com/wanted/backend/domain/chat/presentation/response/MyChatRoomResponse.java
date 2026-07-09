package com.wanted.backend.domain.chat.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wanted.backend.domain.chat.application.result.MyChatRoomDetail;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 채팅방 목록 항목 응답")
public record MyChatRoomResponse(
        @Schema(description = "채팅방 ID", example = "88")
        Long chatRoomId,
        @Schema(description = "채팅방 이름 (스터디 제목)", example = "주말 React 스터디")
        String name,
        @Schema(description = "마지막 메시지 내용 (메시지가 없으면 null)", example = "그럼 일요일 저녁 8시로 정해요!")
        String lastMessage,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss+09:00")
        @Schema(description = "마지막 메시지 전송 시각 (메시지가 없으면 null)", example = "2026-05-11T12:05:00")
        LocalDateTime lastMessageAt,
        @Schema(description = "안 읽은 메시지 수 (현재는 항상 0)", example = "0")
        int unreadCount
) {
    public static MyChatRoomResponse from(MyChatRoomDetail detail) {
        return new MyChatRoomResponse(
                detail.chatRoomId(), detail.name(), detail.lastMessage(),
                detail.lastMessageAt(), detail.unreadCount());
    }
}
