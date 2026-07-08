package com.wanted.backend.domain.chat.application.usecase;

import com.wanted.backend.domain.chat.application.result.SocketTicketResult;

public interface SocketTicketCommandUseCase {
    SocketTicketResult issue(Long memberId);
}
