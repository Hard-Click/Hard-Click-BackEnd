package com.wanted.backend.domain.study.application.event;

public record StudyClosedEvent(
        Long chatRoomId,
        Long studyId
) {}
