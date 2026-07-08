package com.wanted.backend.domain.study.application.port;

import java.util.Optional;

public interface ChatRoomQueryPort {
    Optional<Long> findChatRoomIdByStudyId(Long studyId);
}
