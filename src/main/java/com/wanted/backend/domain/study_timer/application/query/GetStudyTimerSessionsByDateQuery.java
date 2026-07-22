package com.wanted.backend.domain.study_timer.application.query;

import java.time.LocalDate;

public record GetStudyTimerSessionsByDateQuery(
        Long memberId,
        LocalDate date
) {
}
