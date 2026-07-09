package com.wanted.backend.domain.study.application.event;

public record StudyLeftEvent(
        Long chatRoomId,
        Long studyId,
        Long memberId,
        int currentCount
) {}
