package com.wanted.backend.domain.chat.application.event;

public record TypingEvent(
        String type,
        Long memberId,
        String name
) {
    public static TypingEvent of(Long memberId, String name) {
        return new TypingEvent("TYPING", memberId, name);
    }
}
