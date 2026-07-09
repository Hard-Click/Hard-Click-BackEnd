package com.wanted.backend.domain.chat.infrastructure.websocket.message;

import java.util.List;

public record SystemJoinMessage(
        String type,
        String message,
        int participantCount,
        List<ParticipantPresenceMessage> participants
) {
    public static SystemJoinMessage of(String message, List<ParticipantPresenceMessage> participants) {
        return new SystemJoinMessage("SYSTEM_JOIN", message, participants.size(), participants);
    }
}
