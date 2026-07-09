package com.wanted.backend.domain.study.application.command;

public record DeleteStudyCommand(
        Long groupId,
        Long memberId
) {}
