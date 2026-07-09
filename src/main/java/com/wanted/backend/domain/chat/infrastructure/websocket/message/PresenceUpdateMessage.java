package com.wanted.backend.domain.chat.infrastructure.websocket.message;

import java.util.List;

public record PresenceUpdateMessage(
        String type,
        List<ParticipantPresenceMessage> participants
) {
    public static PresenceUpdateMessage of(List<ParticipantPresenceMessage> participants) {
        return new PresenceUpdateMessage("PRESENCE_UPDATE", participants);
    }
}
