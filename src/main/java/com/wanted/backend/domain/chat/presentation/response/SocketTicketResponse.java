package com.wanted.backend.domain.chat.presentation.response;

import com.wanted.backend.domain.chat.application.result.SocketTicketResult;

public record SocketTicketResponse(
        String ticket,
        int expiresInSeconds
) {
    public static SocketTicketResponse from(SocketTicketResult result) {
        return new SocketTicketResponse(result.ticket(), result.expiresInSeconds());
    }
}
