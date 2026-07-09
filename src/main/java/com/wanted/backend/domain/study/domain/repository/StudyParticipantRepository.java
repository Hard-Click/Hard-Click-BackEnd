package com.wanted.backend.domain.study.domain.repository;

import com.wanted.backend.domain.study.domain.model.StudyParticipant;

import java.util.List;

public interface StudyParticipantRepository {
    StudyParticipant save(StudyParticipant studyParticipant);

    List<Long> findMemberIdsByStudyId(Long studyId);

    boolean existsByStudyIdAndMemberId(Long studyId, Long memberId);

    void deleteByStudyIdAndMemberId(Long studyId, Long memberId);
}
