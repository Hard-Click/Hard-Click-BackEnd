package com.wanted.backend.domain.chat.infrastructure.websocket.message;

import java.util.List;

public record SystemKickMessage(
        String type,
        String message,
        Long kickedMemberId,
        int participantCount,
        List<ParticipantPresenceMessage> participants
) {
    public static SystemKickMessage of(String message, Long kickedMemberId, List<ParticipantPresenceMessage> participants) {
        return new SystemKickMessage("SYSTEM_KICK", message, kickedMemberId, participants.size(), participants);
    }
}
