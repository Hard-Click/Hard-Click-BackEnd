package com.wanted.backend.domain.chat.application.result;

public record SocketTicketResult(
        String ticket,
        int expiresInSeconds
) {}
