package com.wanted.backend.domain.study.application.result;

public record JoinStudyResult(
        Long groupId,
        Long chatRoomId,
        int currentCount
) {}
