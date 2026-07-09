package com.wanted.backend.domain.study.application.command;

public record KickStudyMemberCommand(
        Long groupId,
        Long hostId,
        Long targetMemberId
) {}
