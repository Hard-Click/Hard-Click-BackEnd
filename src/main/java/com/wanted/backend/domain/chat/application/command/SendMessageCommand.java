package com.wanted.backend.domain.chat.application.command;

public record SendMessageCommand(
        Long chatRoomId,
        Long senderId,
        String content
) {}
