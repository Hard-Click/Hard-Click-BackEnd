package com.wanted.backend.domain.study.application.event;

public record StudyJoinedEvent(
        Long chatRoomId,
        Long studyId,
        Long memberId,
        int currentCount
) {}
