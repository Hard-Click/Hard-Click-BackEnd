package com.wanted.backend.domain.study_timer.domain.repository;

import com.wanted.backend.domain.study_timer.domain.model.StudyTimerSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyTimerSessionRepository {

    // 특정 날짜(시작 시각 기준)의 종료된(ENDED) 세션 목록 — 타임테이블 막대용. 시작 시각 오름차순.
    List<StudyTimerSession> findEndedSessionsByDate(Long memberId, LocalDate date);

    boolean existsRunningByMemberId(Long memberId);

    boolean existsActiveByMemberId(Long memberId);

    Optional<StudyTimerSession> findRunningByMemberId(Long memberId);

    Optional<StudyTimerSession> findActiveByMemberId(Long memberId);

    Optional<StudyTimerSession> findById(Long sessionId);

    StudyTimerSession save(StudyTimerSession session);
}
