package com.wanted.backend.domain.study.infrastructure.persistence;

import com.wanted.backend.domain.study.domain.model.StudyBannedMember;
import com.wanted.backend.domain.study.domain.repository.StudyBannedMemberRepository;
import org.springframework.stereotype.Repository;

@Repository
public class StudyBannedMemberRepositoryAdapter implements StudyBannedMemberRepository {

    private final SpringDataStudyBannedMemberRepository repository;

    public StudyBannedMemberRepositoryAdapter(SpringDataStudyBannedMemberRepository repository) {
        this.repository = repository;
    }

    @Override
    public StudyBannedMember save(StudyBannedMember studyBannedMember) {
        StudyBannedMemberJpaEntity entity = new StudyBannedMemberJpaEntity(
                studyBannedMember.getStudyId(), studyBannedMember.getMemberId(), studyBannedMember.getBannedAt()
        );
        StudyBannedMemberJpaEntity saved = repository.save(entity);
        return StudyBannedMember.restore(saved.getId(), saved.getStudyId(), saved.getMemberId(), saved.getBannedAt());
    }

    @Override
    public boolean existsByStudyIdAndMemberId(Long studyId, Long memberId) {
        return repository.existsByStudyIdAndMemberId(studyId, memberId);
    }
}
