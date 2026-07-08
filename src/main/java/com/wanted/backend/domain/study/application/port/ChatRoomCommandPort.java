package com.wanted.backend.domain.study.application.port;

public interface ChatRoomCommandPort {
    Long createRoom(Long studyId, Long hostId);
}
