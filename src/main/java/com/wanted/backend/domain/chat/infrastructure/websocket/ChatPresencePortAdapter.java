package com.wanted.backend.domain.chat.infrastructure.websocket;

import com.wanted.backend.domain.chat.application.port.ChatPresencePort;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ChatPresencePortAdapter implements ChatPresencePort {

    private final ChatPresenceTracker presenceTracker;

    public ChatPresencePortAdapter(ChatPresenceTracker presenceTracker) {
        this.presenceTracker = presenceTracker;
    }

    @Override
    public Set<Long> getOnlineMemberIds(Long chatRoomId) {
        return presenceTracker.getOnlineMemberIds(chatRoomId);
    }
}
