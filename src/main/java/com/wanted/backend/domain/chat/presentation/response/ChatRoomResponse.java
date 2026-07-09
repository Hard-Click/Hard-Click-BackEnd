package com.wanted.backend.domain.chat.presentation.response;

import com.wanted.backend.domain.chat.application.result.ChatRoomDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "채팅방 상세 응답")
public record ChatRoomResponse(
        @Schema(description = "채팅방 ID", example = "12")
        Long chatRoomId,
        @Schema(description = "연결된 스터디 모집글 ID", example = "45")
        Long groupId,
        @Schema(description = "스터디 제목", example = "수학 1등급 목표 스터디")
        String title,
        @Schema(description = "과목명", example = "MATH_1")
        String subjectName,
        @Schema(description = "방장 회원 ID", example = "1")
        Long hostId,
        @Schema(description = "채팅방 상태 (ACTIVE, CLOSED)", example = "ACTIVE")
        String status,
        @Schema(description = "참여자 목록")
        List<ParticipantResponse> participants,
        @Schema(description = "참여자 수", example = "2")
        int participantCount
) {
    public static ChatRoomResponse from(ChatRoomDetailResult result) {
        return new ChatRoomResponse(
                result.chatRoomId(), result.groupId(), result.title(), result.subjectName(),
                result.hostId(), result.status(),
                result.participants().stream().map(ParticipantResponse::from).toList(),
                result.participantCount());
    }
}
