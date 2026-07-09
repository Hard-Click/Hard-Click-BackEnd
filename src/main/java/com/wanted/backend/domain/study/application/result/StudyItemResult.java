package com.wanted.backend.domain.study.application.result;

import java.time.LocalDateTime;

public record StudyItemResult(
        Long studyId,
        String title,
        String content,
        String authorName,
        String subjectName,
        int currentCount,
        int maxCount,
        boolean isClosed,
        LocalDateTime createdAt
) {}
