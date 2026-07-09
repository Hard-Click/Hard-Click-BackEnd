package com.wanted.backend.domain.study.application.result;

import java.time.LocalDateTime;
import java.util.List;

public record StudyDetailResult(
        Long groupId,
        String title,
        String content,
        String subjectName,
        String authorName,
        int currentCount,
        int maxCount,
        boolean isMine,
        boolean isJoined,
        boolean isClosed,
        List<String> members,
        Long chatRoomId,
        LocalDateTime createdAt
) {}
