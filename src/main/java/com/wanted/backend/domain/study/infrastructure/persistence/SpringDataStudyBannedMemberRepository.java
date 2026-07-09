package com.wanted.backend.domain.study.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStudyBannedMemberRepository extends JpaRepository<StudyBannedMemberJpaEntity, Long> {
    boolean existsByStudyIdAndMemberId(Long studyId, Long memberId);
}
