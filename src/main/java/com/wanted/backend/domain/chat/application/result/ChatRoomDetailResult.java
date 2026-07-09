package com.wanted.backend.domain.chat.application.result;

import java.util.List;

public record ChatRoomDetailResult(
        Long chatRoomId,
        Long groupId,
        String title,
        String subjectName,
        Long hostId,
        String status,
        List<ParticipantDetail> participants,
        int participantCount
) {}
