package com.wanted.backend.domain.chat.application.usecase;

import com.wanted.backend.domain.chat.application.command.MarkChatRoomReadCommand;

public interface ChatRoomCommandUseCase {
    Long createRoom(Long studyId, Long hostId);

    void addParticipant(Long chatRoomId, Long memberId);

    void closeRoom(Long chatRoomId);

    void removeParticipant(Long chatRoomId, Long memberId);

    void markRead(MarkChatRoomReadCommand command);
}
