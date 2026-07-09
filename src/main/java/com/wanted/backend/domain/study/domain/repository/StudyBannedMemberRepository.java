package com.wanted.backend.domain.study.domain.repository;

import com.wanted.backend.domain.study.domain.model.StudyBannedMember;

public interface StudyBannedMemberRepository {
    StudyBannedMember save(StudyBannedMember studyBannedMember);

    boolean existsByStudyIdAndMemberId(Long studyId, Long memberId);
}
