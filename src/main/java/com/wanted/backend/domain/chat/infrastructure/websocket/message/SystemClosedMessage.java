package com.wanted.backend.domain.chat.infrastructure.websocket.message;

public record SystemClosedMessage(
        String type,
        String message
) {
    private static final String DEFAULT_MESSAGE = "스터디가 해산되어 채팅방이 종료되었습니다";

    public static SystemClosedMessage of() {
        return new SystemClosedMessage("SYSTEM_CLOSED", DEFAULT_MESSAGE);
    }
}
