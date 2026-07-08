package com.wanted.backend.domain.study.application.command;

public record UpdateStudyCommand(
        Long groupId,
        Long memberId,
        String title,
        String subject,
        int maxCount,
        String content
) {}
