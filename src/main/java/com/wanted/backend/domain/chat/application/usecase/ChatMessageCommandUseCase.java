package com.wanted.backend.domain.chat.application.usecase;

import com.wanted.backend.domain.chat.application.command.SendMessageCommand;

public interface ChatMessageCommandUseCase {
    void send(SendMessageCommand command);
}
