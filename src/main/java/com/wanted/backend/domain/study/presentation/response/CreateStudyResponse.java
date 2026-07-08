package com.wanted.backend.domain.study.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스터디 생성 응답")
public record CreateStudyResponse(
        @Schema(description = "생성된 스터디 모집글 ID", example = "45")
        Long groupId,
        @Schema(description = "자동 생성된 채팅방 ID", example = "12")
        Long chatRoomId
) {}
