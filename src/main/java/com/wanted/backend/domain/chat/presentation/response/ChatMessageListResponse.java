package com.wanted.backend.domain.chat.presentation.response;

import com.wanted.backend.domain.chat.application.result.ChatMessageListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "채팅 메시지 목록 응답")
public record ChatMessageListResponse(
        @Schema(description = "메시지 목록 (오래된 순)")
        List<ChatMessageResponse> messages,
        @Schema(description = "다음 페이지(이전 메시지) 존재 여부", example = "true")
        boolean hasNext,
        @Schema(description = "다음 조회에 사용할 커서 ID", example = "300")
        Long nextCursorId
) {
    public static ChatMessageListResponse from(ChatMessageListResult result) {
        return new ChatMessageListResponse(
                result.messages().stream().map(ChatMessageResponse::from).toList(),
                result.hasNext(), result.nextCursorId());
    }
}
