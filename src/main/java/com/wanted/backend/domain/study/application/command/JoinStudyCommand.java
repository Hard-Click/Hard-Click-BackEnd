package com.wanted.backend.domain.study.application.command;

public record JoinStudyCommand(
        Long groupId,
        Long memberId
) {}
