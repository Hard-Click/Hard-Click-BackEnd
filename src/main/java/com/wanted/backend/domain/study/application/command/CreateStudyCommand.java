package com.wanted.backend.domain.study.application.command;

public record CreateStudyCommand(
        Long hostId,
        String title,
        String subject,
        int maxCount,
        String content
) {}
