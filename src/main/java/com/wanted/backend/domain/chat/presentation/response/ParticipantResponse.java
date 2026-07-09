package com.wanted.backend.domain.chat.presentation.response;

import com.wanted.backend.domain.chat.application.result.ParticipantDetail;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 참여자 응답")
public record ParticipantResponse(
        @Schema(description = "참여자 회원 ID", example = "2")
        Long memberId,
        @Schema(description = "마스킹된 참여자 이름", example = "김*민")
        String name,
        @Schema(description = "현재 접속 중인지 여부", example = "false")
        boolean online
) {
    public static ParticipantResponse from(ParticipantDetail detail) {
        return new ParticipantResponse(detail.memberId(), detail.name(), detail.online());
    }
}
