package com.wanted.backend.domain.chat.infrastructure.websocket.message;

public record ParticipantPresenceMessage(
        Long memberId,
        String name,
        boolean online
) {}
