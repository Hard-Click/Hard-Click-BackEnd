package com.wanted.backend.domain.chat.application.usecase;

import com.wanted.backend.domain.chat.application.result.SocketTicketResult;

import java.util.Optional;

public interface SocketTicketCommandUseCase {
    SocketTicketResult issue(Long memberId);

    Optional<Long> consume(String ticket);
}
