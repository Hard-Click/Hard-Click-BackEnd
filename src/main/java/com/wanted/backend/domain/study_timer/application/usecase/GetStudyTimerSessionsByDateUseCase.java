package com.wanted.backend.domain.study_timer.application.usecase;

import com.wanted.backend.domain.study_timer.application.query.GetStudyTimerSessionsByDateQuery;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 특정 날짜의 종료된(ENDED) 순공시간 세션 목록 조회 — 타임테이블에 실제 공부 시간대를 막대로 그리기 위함.
 *
 * <p>진행 중(RUNNING/PAUSED) 세션은 {@code GET /sessions/current}로, 중단(CANCELED) 세션은 제외한다.
 * 세션은 시작 시각(startedAt) 기준으로 해당 날짜에 속하는 것만 반환한다(자정을 걸치면 시작한 날).
 */
public interface GetStudyTimerSessionsByDateUseCase {

    List<StudyTimerSessionView> handle(GetStudyTimerSessionsByDateQuery query);

    record StudyTimerSessionView(
            Long sessionId,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt
    ) {
    }
}
