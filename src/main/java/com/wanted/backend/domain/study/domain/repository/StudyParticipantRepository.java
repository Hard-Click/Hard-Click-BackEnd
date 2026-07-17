package com.wanted.backend.domain.study.domain.repository;

import com.wanted.backend.domain.study.domain.model.StudyParticipant;

import java.util.Collection;
import java.util.List;

public interface StudyParticipantRepository {
    StudyParticipant save(StudyParticipant studyParticipant);

    List<Long> findMemberIdsByStudyId(Long studyId);

    boolean existsByStudyIdAndMemberId(Long studyId, Long memberId);

    void deleteByStudyIdAndMemberId(Long studyId, Long memberId);

    // 목록 화면의 참여 여부(isJoined) 표시용 — 페이지의 스터디 ID들을 IN 조회 1번으로 모아
    // 스터디마다 existsBy를 반복 호출하는 N+1을 피한다.
    List<Long> findStudyIdsByMemberIdAndStudyIdIn(Long memberId, Collection<Long> studyIds);
}
