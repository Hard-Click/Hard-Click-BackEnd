package com.wanted.backend.domain.chat.application.command;

public record MarkChatRoomReadCommand(
        Long chatRoomId,
        Long memberId,
        Long lastReadMessageId
) {}
