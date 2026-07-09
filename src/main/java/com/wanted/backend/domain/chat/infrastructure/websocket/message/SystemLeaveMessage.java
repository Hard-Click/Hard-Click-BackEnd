package com.wanted.backend.domain.chat.infrastructure.websocket.message;

import java.util.List;

public record SystemLeaveMessage(
        String type,
        String message,
        int participantCount,
        List<ParticipantPresenceMessage> participants
) {
    public static SystemLeaveMessage of(String message, List<ParticipantPresenceMessage> participants) {
        return new SystemLeaveMessage("SYSTEM_LEAVE", message, participants.size(), participants);
    }
}
