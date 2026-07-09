package com.wanted.backend.domain.chat.application.usecase;

public interface ChatRoomCommandUseCase {
    Long createRoom(Long studyId, Long hostId);

    void addParticipant(Long chatRoomId, Long memberId);
}
