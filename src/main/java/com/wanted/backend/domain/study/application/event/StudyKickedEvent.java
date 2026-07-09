package com.wanted.backend.domain.study.application.event;

public record StudyKickedEvent(
        Long chatRoomId,
        Long studyId,
        Long kickedMemberId,
        int currentCount
) {}
