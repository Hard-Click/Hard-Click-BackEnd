package com.wanted.backend.domain.chat.application.port;

import java.util.Set;

public interface ChatPresencePort {
    Set<Long> getOnlineMemberIds(Long chatRoomId);
}
