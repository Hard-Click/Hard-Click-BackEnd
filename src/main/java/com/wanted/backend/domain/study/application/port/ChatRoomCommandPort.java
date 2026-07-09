package com.wanted.backend.domain.study.application.port;

public interface ChatRoomCommandPort {
    Long createRoom(Long studyId, Long hostId);

    void addParticipant(Long chatRoomId, Long memberId);

    void removeParticipant(Long chatRoomId, Long memberId);
}
