package com.wanted.backend.domain.study.application.command;

public record LeaveStudyCommand(
        Long groupId,
        Long memberId
) {}
