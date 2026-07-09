package com.wanted.backend.domain.chat.application.result;

public record ParticipantDetail(
        Long memberId,
        String name,
        boolean online
) {}
