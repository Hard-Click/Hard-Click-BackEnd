package com.wanted.backend.domain.chat.presentation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wanted.backend.domain.chat.application.result.ChatMessageDetail;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅 메시지 응답")
public record ChatMessageResponse(
        @Schema(description = "메시지 종류 (CHAT, SYSTEM_JOIN, SYSTEM_LEAVE)", example = "CHAT")
        String type,
        @Schema(description = "메시지 ID", example = "301")
        Long messageId,
        @Schema(description = "발신자 ID (시스템 메시지는 null)", example = "2")
        Long senderId,
        @Schema(description = "발신자 이름 (시스템 메시지는 null)", example = "김*민")
        String senderName,
        @Schema(description = "메시지 내용", example = "안녕하세요!")
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss+09:00")
        @Schema(description = "전송 시각", example = "2026-07-07T21:00:00")
        LocalDateTime sentAt
) {
    public static ChatMessageResponse from(ChatMessageDetail detail) {
        return new ChatMessageResponse(
                detail.type(), detail.messageId(), detail.senderId(), detail.senderName(),
                detail.content(), detail.sentAt());
    }
}
