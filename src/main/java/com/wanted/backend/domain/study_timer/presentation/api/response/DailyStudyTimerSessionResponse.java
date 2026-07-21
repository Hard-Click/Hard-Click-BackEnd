package com.wanted.backend.domain.study_timer.presentation.api.response;

import com.wanted.backend.domain.study_timer.application.usecase.GetStudyTimerSessionsByDateUseCase.StudyTimerSessionView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "특정 날짜의 종료된 순공시간 세션 항목 (타임테이블 막대용)")
public record DailyStudyTimerSessionResponse(
        @Schema(description = "세션 ID", example = "55")
        Long sessionId,

        @Schema(description = "시작 시각", example = "2026-07-21T09:00:00+09:00")
        OffsetDateTime startedAt,

        @Schema(description = "종료 시각", example = "2026-07-21T10:30:00+09:00")
        OffsetDateTime endedAt
) {
    public static DailyStudyTimerSessionResponse from(StudyTimerSessionView view) {
        return new DailyStudyTimerSessionResponse(view.sessionId(), view.startedAt(), view.endedAt());
    }
}
