package com.wanted.backend.domain.study.presentation.response;

import com.wanted.backend.domain.study.application.result.JoinStudyResult;

public record JoinStudyResponse(
        Long groupId,
        Long chatRoomId,
        int currentCount
) {
    public static JoinStudyResponse from(JoinStudyResult result) {
        return new JoinStudyResponse(result.groupId(), result.chatRoomId(), result.currentCount());
    }
}
